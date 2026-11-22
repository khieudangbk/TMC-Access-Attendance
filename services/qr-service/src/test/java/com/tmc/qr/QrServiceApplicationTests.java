package com.tmc.qr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.tmc.qr.client.EmployeeAccess;
import com.tmc.qr.config.QrSigningKeys;
import com.tmc.qr.dto.response.IssuedQrDTO;
import com.tmc.qr.service.QrService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.config.location=classpath:qr-test.yaml",
        "tmc.internal.enabled=true", "tmc.internal.client-id=service-qr-test", "tmc.internal.scopes=identity.verify"})
@AutoConfigureMockMvc
class QrServiceApplicationTests {
    @org.junit.jupiter.api.Test
    void qrRevocationAuditRetainsSiteAndValidEntityWithoutRawToken() {
        UUID employeeId = UUID.randomUUID();
        UUID factoryId = UUID.randomUUID();
        qrService.revoke(employeeId, factoryId);
        String payload = jdbcTemplate.queryForObject("SELECT payload::text FROM integration_outbox WHERE payload->>'action' = 'QR_REVOKED' ORDER BY created_at DESC LIMIT 1", String.class);
        assertTrue(payload.contains(factoryId.toString()));
        assertTrue(payload.contains(employeeId.toString()));
        assertFalse(payload.contains("qrToken"));
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbcTemplate.update("DELETE FROM qr_audit_events"));
    }

    private static final EmbeddedPostgres POSTGRES = startPostgres();
    private static final RSAKey ACTIVE_KEY = newKey("active");
    private static final RSAKey PREVIOUS_KEY = newKey("previous");
    private static final Path KEY_FILE = keyFile();
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final UUID FACTORY_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-21T08:30:00Z");

    @Autowired
    QrService qrService;
    @Autowired
    QrSigningKeys keys;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    Clock clock;
    @MockitoBean
    EmployeeAccess employeeAccess;
    @MockitoBean
    JwtDecoder jwtDecoder;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("tmc.qr.jwk-set-file", () -> KEY_FILE.toUri().toString());
    }

    static EmbeddedPostgres startPostgres() {
        try {
            return EmbeddedPostgres.start();
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    static RSAKey newKey(String id) {
        try {
            return new RSAKeyGenerator(2048).keyID(id).generate();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    static Path keyFile() {
        try {
            Path path = Files.createTempFile("tmc-qr-key-test-", ".json");
            Files.writeString(path, new JWKSet(List.of(ACTIVE_KEY, PREVIOUS_KEY.toPublicJWK())).toString(false));
            return path;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @AfterAll
    static void close() throws IOException {
        POSTGRES.close();
        Files.deleteIfExists(KEY_FILE);
    }

    @BeforeEach
    void reset() {
        jdbcTemplate.execute("TRUNCATE qr_credentials, qr_audit_events");
        when(clock.instant()).thenReturn(NOW);
        when(employeeAccess.allowed(any(), any())).thenReturn(true);
    }

    @Test
    void issuedQrIsSignedAndContainsOnlyRequiredReferences() throws Exception {
        var issued = qrService.issue(EMPLOYEE_ID, FACTORY_ID);
        var result = qrService.verify(issued.token(), FACTORY_ID);
        assertTrue(result.valid());
        assertEquals(EMPLOYEE_ID, result.employeeId());
        assertEquals(NOW.plusSeconds(60), result.expiresAt());
        var claims = SignedJWT.parse(issued.token()).getJWTClaimsSet();
        assertFalse(claims.getClaims().containsKey("fullName"));
        assertFalse(issued.toString().contains(issued.token()));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT count(*) FROM qr_credentials", Integer.class));
    }

    @Test
    void expiredAndWrongFactoryQrAreDenied() {
        var issued = qrService.issue(EMPLOYEE_ID, FACTORY_ID);
        assertEquals("QR_FACTORY_MISMATCH", qrService.verify(issued.token(), UUID.randomUUID()).reason());
        when(clock.instant()).thenReturn(NOW.plusSeconds(60));
        assertEquals("QR_EXPIRED", qrService.verify(issued.token(), FACTORY_ID).reason());
    }

    @Test
    void revokeAndReissueInvalidateOldCredential() {
        var first = qrService.issue(EMPLOYEE_ID, FACTORY_ID);
        var second = qrService.issue(EMPLOYEE_ID, FACTORY_ID);
        assertFalse(qrService.verify(first.token(), FACTORY_ID).valid());
        assertTrue(qrService.verify(second.token(), FACTORY_ID).valid());
        qrService.revoke(EMPLOYEE_ID, FACTORY_ID);
        assertFalse(qrService.verify(second.token(), FACTORY_ID).valid());
        assertEquals(3, jdbcTemplate.queryForObject("SELECT count(*) FROM qr_audit_events", Integer.class));
    }

    @Test
    void disabledEmployeeOrDependencyFailureCannotIssue() {
        when(employeeAccess.allowed(any(), any())).thenReturn(false);
        assertThrows(ResponseStatusException.class, () -> qrService.issue(EMPLOYEE_ID, FACTORY_ID));
        when(employeeAccess.allowed(any(), any())).thenThrow(new IllegalStateException("unavailable"));
        assertThrows(IllegalStateException.class, () -> qrService.issue(EMPLOYEE_ID, FACTORY_ID));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT count(*) FROM qr_credentials", Integer.class));
    }

    @Test
    void forgedSignatureWrongPurposeAndUnregisteredCredentialAreDenied() throws Exception {
        var issued = qrService.issue(EMPLOYEE_ID, FACTORY_ID);
        var claims = SignedJWT.parse(issued.token()).getJWTClaimsSet();
        assertFalse(qrService.verify(sign(claims, newKey("attacker"), "tmc-qr+jwt"), FACTORY_ID).valid());
        assertFalse(qrService.verify(sign(claims, ACTIVE_KEY, "JWT"), FACTORY_ID).valid());
        var wrongPurpose = new JWTClaimsSet.Builder(claims).claim("purpose", "api-access").build();
        assertEquals("INVALID_QR_PURPOSE", qrService.verify(sign(wrongPurpose, ACTIVE_KEY, "tmc-qr+jwt"), FACTORY_ID).reason());
        var unknown = new JWTClaimsSet.Builder(claims).jwtID(UUID.randomUUID().toString()).build();
        assertEquals("QR_REVOKED_OR_UNKNOWN", qrService.verify(sign(unknown, ACTIVE_KEY, "tmc-qr+jwt"), FACTORY_ID).reason());
        assertFalse(qrService.verify("not-a-jwt", FACTORY_ID).valid());
    }

    @Test
    void previousPublicKeyCanVerifyExistingCredentialDuringRotation() throws Exception {
        var issued = qrService.issue(EMPLOYEE_ID, FACTORY_ID);
        var claims = SignedJWT.parse(issued.token()).getJWTClaimsSet();
        jdbcTemplate.update("UPDATE qr_credentials SET key_id = 'previous' WHERE qr_id = ?", issued.qrId());
        assertTrue(qrService.verify(sign(claims, PREVIOUS_KEY, "tmc-qr+jwt"), FACTORY_ID).valid());
        assertTrue(JWKSet.parse(keys.publicJwks()).getKeys().stream().noneMatch(key -> key.isPrivate()));
    }

    @Test
    void httpRequiresScopesAndEmployeeIdentity() throws Exception {
        String body = "{\"factoryId\":\"" + FACTORY_ID + "\"}";
        mockMvc.perform(post("/me/qr").contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/me/qr").with(jwt()).contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        var user = jwt().jwt(token -> token.subject(EMPLOYEE_ID.toString()).claim("employee_id", EMPLOYEE_ID.toString()))
                .authorities(new SimpleGrantedAuthority("SCOPE_qr.issue"));
        mockMvc.perform(post("/me/qr").with(user).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.qrId").exists());
        mockMvc.perform(get("/internal/qr/jwks").with(user)).andExpect(status().isForbidden());
        mockMvc.perform(get("/internal/qr/jwks")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_qr.verify"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.data").doesNotExist());
        mockMvc.perform(delete("/me/qr").with(user).contentType("application/json").content(body))
                .andExpect(status().isOk());
    }

    @Test
    void invalidRequestDoesNotEchoRawQrInErrorBody() throws Exception {
        var verifier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_qr.verify"));
        String token = "private-token-must-not-be-returned";
        String response = mockMvc.perform(post("/internal/qr/verifications").with(verifier)
                        .contentType("application/json").content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        assertFalse(response.contains(token));
    }

    private String sign(JWTClaimsSet claims, RSAKey key, String type) throws Exception {
        var token = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID())
                .type(new JOSEObjectType(type)).build(), claims);
        token.sign(new RSASSASigner(key));
        return token.serialize();
    }

    @Test
    void concurrentIssuanceLeavesOnlyOneActiveCredential() throws Exception {
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(4)) {
            var tasks = new java.util.ArrayList<java.util.concurrent.Callable<IssuedQrDTO>>();
            for (int index = 0; index < 4; index++) {
                tasks.add(() -> qrService.issue(EMPLOYEE_ID, FACTORY_ID));
            }
            int activeCredentials = 0;
            for (var result : executor.invokeAll(tasks)) {
                if (qrService.verify(result.get().token(), FACTORY_ID).valid()) {
                    activeCredentials++;
                }
            }
            assertEquals(1, activeCredentials);
            assertEquals(4, jdbcTemplate.queryForObject("SELECT count(*) FROM qr_credentials", Integer.class));
        }
    }
}
