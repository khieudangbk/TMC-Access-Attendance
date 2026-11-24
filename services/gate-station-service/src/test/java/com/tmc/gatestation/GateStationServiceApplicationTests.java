package com.tmc.gatestation;

import java.io.IOException;
import java.util.UUID;

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

import com.tmc.gatestation.entity.Direction;
import com.tmc.gatestation.entity.Station;
import com.tmc.gatestation.service.StationService;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.config.location=classpath:station-test.yaml")
@AutoConfigureMockMvc
class GateStationServiceApplicationTests {

    private static final EmbeddedPostgres POSTGRES = startPostgres();
    private static final UUID FACTORY_ID = UUID.randomUUID();
    @Autowired
    StationService stationService;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    JwtDecoder jwtDecoder;

    private Station station;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    static EmbeddedPostgres startPostgres() {
        try {
            return EmbeddedPostgres.start();
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @AfterAll
    static void close() throws IOException {
        POSTGRES.close();
    }

    @BeforeEach
    void seed() {
        jdbcTemplate.execute("TRUNCATE factories, station_audit_events CASCADE");
        stationService.createFactory(FACTORY_ID, "BD", "Binh Duong", "administrator");
        station = stationService.createStation(FACTORY_ID, "BD-ENTRY", "kiosk-bd-1", Direction.ENTRY,
                "turnstile-controller-1", "administrator");
    }

    @Test
    void trustedStationSuppliesFactoryAndDirection() {
        var check = stationService.verify(station.stationId(), "kiosk-bd-1");
        assertTrue(check.allowed());
        assertEquals(FACTORY_ID, check.factoryId());
        assertEquals(Direction.ENTRY, check.direction());
        assertEquals(1, check.revision());
    }

    @Test
    void wrongDeviceDisabledStationAndDisabledFactoryAreDenied() {
        assertEquals("DEVICE_STATION_MISMATCH", stationService.verify(station.stationId(), "kiosk-other").reason());
        stationService.setActive(station.stationId(), false, 1, "administrator");
        assertFalse(stationService.verify(station.stationId(), "kiosk-bd-1").allowed());
        stationService.setActive(station.stationId(), true, 2, "administrator");
        stationService.setFactoryActive(FACTORY_ID, false, "administrator");
        assertFalse(stationService.verify(station.stationId(), "kiosk-bd-1").allowed());
    }

    @Test
    void concurrentConfigurationCannotSilentlyOverwriteNewRevision() {
        stationService.setActive(station.stationId(), false, 1, "administrator");
        var exception = assertThrows(ResponseStatusException.class,
                () -> stationService.setActive(station.stationId(), true, 1, "administrator"));
        assertEquals(409, exception.getStatusCode().value());
        assertEquals(2, stationService.station(station.stationId()).revision());
    }

    @Test
    void uniquenessAndFactoryOwnershipAreEnforcedByDatabase() {
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> stationService.createStation(
                FACTORY_ID, "OTHER", "kiosk-bd-1", Direction.EXIT, "other-controller", "administrator"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> stationService.createStation(
                UUID.randomUUID(), "OTHER", "kiosk-other", Direction.EXIT, "other-controller", "administrator"));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT count(*) FROM stations", Integer.class));
    }

    @Test
    void managementAndVerificationHaveDifferentScopes() throws Exception {
        String path = "/internal/stations/" + station.stationId() + "/checks";
        mockMvc.perform(post(path).contentType("application/json").content("{\"deviceId\":\"kiosk-bd-1\"}"))
                .andExpect(status().isUnauthorized());
        var verifier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_stations.verify"));
        mockMvc.perform(post(path).with(verifier).contentType("application/json").content("{\"deviceId\":\"kiosk-bd-1\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.allowed").value(true));
        mockMvc.perform(post("/factories").with(verifier).contentType("application/json")
                        .content("{\"code\":\"LA\",\"name\":\"Long An\"}"))
                .andExpect(status().isForbidden());
        var manager = jwt().authorities(new SimpleGrantedAuthority("SCOPE_stations.manage"));
        mockMvc.perform(post("/factories").with(manager).contentType("application/json")
                        .content("{\"code\":\"LA\",\"name\":\"Long An\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void heartbeatRequiresDeviceSubjectAndSignedStationBinding() throws Exception {
        String path = "/stations/" + station.stationId() + "/heartbeat";
        var kiosk = jwt().jwt(token -> token.subject("kiosk-bd-1").claim("station_id", station.stationId().toString()))
                .authorities(new SimpleGrantedAuthority("SCOPE_access.scan"));
        mockMvc.perform(post(path).with(kiosk)).andExpect(status().isOk());
        assertNotNull(jdbcTemplate.queryForObject("SELECT last_seen_at FROM stations", java.sql.Timestamp.class));
        var wrongStation = jwt().jwt(token -> token.subject("kiosk-bd-1").claim("station_id", UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("SCOPE_access.scan"));
        mockMvc.perform(post(path).with(wrongStation)).andExpect(status().isForbidden());
        stationService.setActive(station.stationId(), false, 1, "administrator");
        mockMvc.perform(post(path).with(kiosk)).andExpect(status().isForbidden());
    }

    @Test
    void stationChangesAreAuditedInSameTransaction() {
        stationService.setActive(station.stationId(), false, 1, "administrator");
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT count(*) FROM station_audit_events WHERE target_id = ? AND event_type = 'STATION_DISABLED'
                """, Integer.class, station.stationId()));
    }
}
