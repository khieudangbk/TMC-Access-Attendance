package com.tmc.identityuser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.tmc.identityuser.entity.Employee;
import com.tmc.identityuser.entity.Employee.Role;
import com.tmc.identityuser.repository.IdentityStore;
import com.tmc.identityuser.service.IdentityService;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.config.location=classpath:identity-test.yaml")
@AutoConfigureMockMvc
class IdentityUserServiceApplicationTests {

    private static final EmbeddedPostgres POSTGRES = startPostgres();
    private static final RSAKey OLD_KEY = createKey("previous-test-key");
    private static final RSAKey ACTIVE_KEY = createKey("active-test-key");
    private static final Path KEY_FILE = writeKeyFile();
    private static final String PASSWORD = "employee-test-password";
    private static final UUID FACTORY_ID = UUID.randomUUID();

    @Autowired
    private IdentityService identityService;
    @Autowired
    private IdentityStore identityStore;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtDecoder jwtDecoder;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RegisteredClientRepository clients;

    private Employee admin;
    private Employee employee;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("tmc.identity.jwk-set-file", () -> KEY_FILE.toUri().toString());
    }

    private static EmbeddedPostgres startPostgres() {
        try {
            return EmbeddedPostgres.start();
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static RSAKey createKey(String keyId) {
        try {
            return new RSAKeyGenerator(2048).keyID(keyId).keyUse(KeyUse.SIGNATURE).generate();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static Path writeKeyFile() {
        try {
            Path path = Files.createTempFile("tmc-identity-test-key-", ".json");
            Files.writeString(path, new JWKSet(List.of(ACTIVE_KEY, OLD_KEY.toPublicJWK())).toString(false));
            return path;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @AfterAll
    static void cleanup() throws IOException {
        POSTGRES.close();
        Files.deleteIfExists(KEY_FILE);
    }

    @BeforeEach
    void seedEmployees() {
        jdbcTemplate.execute("TRUNCATE employees CASCADE");
        admin = identityService.createEmployee("ADMIN", "Test Administrator", "admin", PASSWORD,
                Set.of(Role.ADMIN), Set.of(), "test-fixture");
        employee = identityService.createEmployee("EMP-001", "Test Employee", "employee", PASSWORD,
                Set.of(Role.EMPLOYEE), Set.of(FACTORY_ID), admin.employeeId().toString());
    }

    @Test
    void passwordsAreHashedAndNotIncludedInApiResponse() throws Exception {
        String passwordHash = jdbcTemplate.queryForObject("SELECT password_hash FROM employees WHERE employee_id = ?",
                String.class, employee.employeeId());
        assertNotEquals(PASSWORD, passwordHash);
        assertTrue(passwordEncoder.matches(PASSWORD, passwordHash));
        String response = mockMvc.perform(get("/employees/" + employee.employeeId())
                        .with(jwt().jwt(token -> token.subject(employee.employeeId().toString()))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertFalse(response.contains("password"));
        assertFalse(response.contains(passwordHash));
    }

    @Test
    void employeeCannotReadOtherEmployeeOrEscalatePermissions() throws Exception {
        var actor = jwt().jwt(token -> token.subject(employee.employeeId().toString()).claim("roles", List.of("ADMIN")));
        mockMvc.perform(get("/employees/" + admin.employeeId()).with(actor)).andExpect(status().isForbidden());
        mockMvc.perform(put("/employees/" + employee.employeeId() + "/permissions").with(actor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"ADMIN\"],\"factoryIds\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalAccessCheckEnforcesScopeAndCurrentFactoryGrant() throws Exception {
        String payload = "{\"employeeId\":\"" + employee.employeeId() + "\",\"factoryId\":\"" + FACTORY_ID + "\"}";
        mockMvc.perform(post("/internal/access-checks").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());
        var verifier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_identity.verify"));
        mockMvc.perform(post("/internal/access-checks").with(verifier).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.allowed").value(true));
        String wrongSite = payload.replace(FACTORY_ID.toString(), UUID.randomUUID().toString());
        mockMvc.perform(post("/internal/access-checks").with(verifier).contentType(MediaType.APPLICATION_JSON).content(wrongSite))
                .andExpect(jsonPath("$.data.allowed").value(false));
        identityService.updatePermissions(employee.employeeId(), Set.of(Role.EMPLOYEE), Set.of(), admin.employeeId().toString());
        mockMvc.perform(post("/internal/access-checks").with(verifier).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(jsonPath("$.data.reason").value("SITE_ACCESS_DENIED"));
    }

    @Test
    void disabledEmployeeCannotLogInOrUseExistingIdentityJwt() throws Exception {
        identityService.setActive(employee.employeeId(), false, admin.employeeId().toString());
        mockMvc.perform(formLogin().user("employee").password(PASSWORD)).andExpect(redirectedUrl("/login?error"));
        mockMvc.perform(get("/employees/" + employee.employeeId())
                        .with(jwt().jwt(token -> token.subject(employee.employeeId().toString()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void lastAdminCannotBeDisabledOrDemoted() {
        assertThrows(ResponseStatusException.class,
                () -> identityService.setActive(admin.employeeId(), false, admin.employeeId().toString()));
        assertThrows(ResponseStatusException.class, () -> identityService.updatePermissions(
                admin.employeeId(), Set.of(Role.EMPLOYEE), Set.of(), admin.employeeId().toString()));
        assertTrue(identityStore.employee(admin.employeeId()).active());
    }

    @Test
    void kioskTokenUsesRegisteredStationAndDisabledKioskCannotIssueTokens() throws Exception {
        String clientId = "kiosk-" + UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        identityService.registerDevice(clientId, PASSWORD, stationId, admin.employeeId().toString());
        String token = token(clientId, PASSWORD, "access.scan access.confirm");
        var claims = jwtDecoder.decode(token);
        assertEquals(clientId, claims.getSubject());
        assertEquals(stationId.toString(), claims.getClaimAsString("station_id"));
        assertEquals("active-test-key", claims.getHeaders().get("kid"));
        assertTrue(claims.getExpiresAt().getEpochSecond() - claims.getIssuedAt().getEpochSecond() <= 60);
        assertFalse(clients.findByClientId(clientId).getClientSecret().contains(PASSWORD));
        assertTrue(jdbcTemplate.queryForObject("SELECT count(*) FROM oauth2_authorization", Integer.class) > 0);
        identityService.setDeviceActive(clientId, false, admin.employeeId().toString());
        mockMvc.perform(post("/oauth2/token").with(httpBasic(clientId, PASSWORD))
                        .param("grant_type", "client_credentials").param("scope", "access.scan"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void kioskCannotRequestServiceVerifierScope() throws Exception {
        String clientId = "kiosk-" + UUID.randomUUID();
        identityService.registerDevice(clientId, PASSWORD, UUID.randomUUID(), admin.employeeId().toString());
        mockMvc.perform(post("/oauth2/token").with(httpBasic(clientId, PASSWORD))
                        .param("grant_type", "client_credentials").param("scope", "identity.verify"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("invalid_scope"));
    }

    @Test
    void jwksPublishesOnlyPublicKeysAndOldPublicKeyVerifiesDuringRotation() throws Exception {
        String body = mockMvc.perform(get("/oauth2/jwks")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JWKSet publicKeys = JWKSet.parse(body);
        assertEquals(2, publicKeys.getKeys().size());
        assertTrue(publicKeys.getKeys().stream().noneMatch(key -> key.isPrivate()));
        String oldToken = signedToken(OLD_KEY, Instant.now().plusSeconds(60));
        assertEquals("rotation-test", jwtDecoder.decode(oldToken).getSubject());
        assertThrows(JwtException.class, () -> jwtDecoder.decode(signedToken(OLD_KEY, Instant.now().minusSeconds(120))));
        assertThrows(JwtException.class, () -> jwtDecoder.decode(signedToken(createKey("attacker"), Instant.now().plusSeconds(60))));
    }

    @Test
    void authorizationCodeRequiresPkceAndLoginUsesPersistedPassword() throws Exception {
        var login = mockMvc.perform(formLogin().user("employee").password(PASSWORD))
                .andExpect(status().is3xxRedirection()).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);
        mockMvc.perform(get("/oauth2/authorize").session(session)
                        .queryParam("response_type", "code").queryParam("client_id", "tmc-web")
                        .queryParam("redirect_uri", "https://client.test/callback").queryParam("scope", "qr.issue"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("error=invalid_request")));
        String verifier = "test-pkce-verifier-with-at-least-forty-three-characters";
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                java.security.MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
        String redirect = mockMvc.perform(get("/oauth2/authorize").session(session)
                        .queryParam("response_type", "code").queryParam("client_id", "tmc-web")
                        .queryParam("redirect_uri", "https://client.test/callback").queryParam("scope", "qr.issue")
                        .queryParam("code_challenge", challenge).queryParam("code_challenge_method", "S256"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertNotNull(redirect);
        String code = java.util.Arrays.stream(java.net.URI.create(redirect).getRawQuery().split("&"))
                .filter(parameter -> parameter.startsWith("code=")).map(parameter -> parameter.substring(5))
                .map(value -> java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8)).findFirst().orElseThrow();
        String response = mockMvc.perform(post("/oauth2/token").param("grant_type", "authorization_code")
                        .param("client_id", "tmc-web").param("code", code).param("code_verifier", verifier)
                        .param("redirect_uri", "https://client.test/callback"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var token = jwtDecoder.decode(new tools.jackson.databind.ObjectMapper().readTree(response).get("access_token").asText());
        assertEquals(employee.employeeId().toString(), token.getSubject());
        assertEquals(List.of("EMPLOYEE"), token.getClaimAsStringList("roles"));
        assertEquals(List.of(FACTORY_ID.toString()), token.getClaimAsStringList("factory_ids"));
    }

    @Test
    void identityMutationsHaveDurableAuditWithoutPassword() {
        identityService.updatePermissions(employee.employeeId(), Set.of(Role.EMPLOYEE), Set.of(), admin.employeeId().toString());
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT count(*) FROM identity_audit_events WHERE target_id = ?
                AND actor_id = ? AND event_type = 'PERMISSIONS_CHANGED'
                """, Integer.class, employee.employeeId().toString(), admin.employeeId().toString()));
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbcTemplate.update("DELETE FROM identity_audit_events"));
    }

    @Test
    void administrationListsUpdatesAndValidatesWithoutLeakingSecrets() throws Exception {
        var administrator = jwt().jwt(token -> token.subject(admin.employeeId().toString()));
        mockMvc.perform(get("/employees").with(administrator).queryParam("limit", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(2)).andExpect(jsonPath("$.data.limit").value(2));
        mockMvc.perform(get("/employees").with(administrator).queryParam("limit", "1000"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/employees/" + employee.employeeId() + "/profile").with(administrator)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"Updated employee\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.fullName").value("Updated employee"));
        String body = mockMvc.perform(post("/employees").with(administrator).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"do-not-return-this-password\"}"))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        assertFalse(body.contains("do-not-return-this-password"));
    }

    @Test
    void signedTechnicalTokenCanVerifyButCannotManageEmployees() throws Exception {
        String accessToken = token("test-service", "test-service-secret-123", "identity.verify");
        String payload = "{\"employeeId\":\"" + employee.employeeId() + "\",\"factoryId\":\"" + FACTORY_ID + "\"}";
        mockMvc.perform(post("/internal/access-checks").header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.allowed").value(true));
        mockMvc.perform(get("/employees").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void reportAuthorizationUsesCurrentRoleAndFactoryGrant() throws Exception {
        var verifier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_identity.verify"));
        String request = "{\"employeeId\":\"" + employee.employeeId() + "\",\"factoryId\":\"" + FACTORY_ID
                + "\",\"roles\":[\"HR\"]}";
        mockMvc.perform(post("/internal/authorization-checks").with(verifier).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.allowed").value(false));
        identityService.updatePermissions(employee.employeeId(), Set.of(Role.HR), Set.of(FACTORY_ID), admin.employeeId().toString());
        mockMvc.perform(post("/internal/authorization-checks").with(verifier).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.allowed").value(true));
        mockMvc.perform(get("/internal/factories/" + FACTORY_ID + "/employees").with(verifier))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0]").value(employee.employeeId().toString()));
        identityService.updatePermissions(employee.employeeId(), Set.of(Role.HR), Set.of(), admin.employeeId().toString());
        mockMvc.perform(post("/internal/authorization-checks").with(verifier).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.allowed").value(false));
        mockMvc.perform(post("/internal/authorization-checks").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
    }

    private String token(String clientId, String secret, String scope) throws Exception {
        String response = mockMvc.perform(post("/oauth2/token").with(httpBasic(clientId, secret))
                        .param("grant_type", "client_credentials").param("scope", scope))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        // Extract only inside the test process; never write token values to logs.
        return new tools.jackson.databind.ObjectMapper().readTree(response).get("access_token").asText();
    }

    @Test
    void serviceSpecificCredentialsAreLeastPrivilegeAndCanBeDisabled() throws Exception {
        String clientId = "service-qr-" + UUID.randomUUID();
        identityService.registerServiceClient(clientId, PASSWORD, Set.of("identity.verify"), admin.employeeId().toString());
        assertEquals(clientId, jwtDecoder.decode(token(clientId, PASSWORD, "identity.verify")).getSubject());
        mockMvc.perform(post("/oauth2/token").with(httpBasic(clientId, PASSWORD))
                        .param("grant_type", "client_credentials").param("scope", "stations.manage"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("invalid_scope"));
        identityService.setServiceActive(clientId, false, admin.employeeId().toString());
        mockMvc.perform(post("/oauth2/token").with(httpBasic(clientId, PASSWORD))
                        .param("grant_type", "client_credentials").param("scope", "identity.verify"))
                .andExpect(status().isUnauthorized());
        assertThrows(ResponseStatusException.class, () -> identityService.registerServiceClient(
                "service-invalid", PASSWORD, Set.of("access.scan"), admin.employeeId().toString()));
    }

    private String signedToken(RSAKey key, Instant expiresAt) throws Exception {
        var claims = new JWTClaimsSet.Builder().subject("rotation-test").issuer("http://identity.test")
                .issueTime(java.util.Date.from(Instant.now().minusSeconds(180)))
                .expirationTime(java.util.Date.from(expiresAt)).build();
        var token = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        token.sign(new RSASSASigner(key));
        return token.serialize();
    }
}
