package com.tmc.accessevent;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.tmc.accessevent.client.ReviewAccess;
import com.tmc.accessevent.dto.response.ConfirmationResultDTO;
import com.tmc.accessevent.dto.response.ScanDecisionDTO;
import com.tmc.accessevent.entity.GateCommand.Outcome;
import com.tmc.accessevent.entity.VerifiedAccess;
import com.tmc.accessevent.messaging.OutboxPublisher;
import com.tmc.accessevent.service.AccessPolicy;
import com.tmc.accessevent.service.AccessService;
import com.tmc.accessevent.service.EvidenceReviewService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.config.location=classpath:access-test.yaml",
        "tmc.internal.enabled=true", "tmc.internal.client-id=service-access-test",
        "tmc.internal.scopes=identity.verify qr.verify stations.verify"})
@AutoConfigureMockMvc
class AccessEventServiceApplicationTests {

    private static final EmbeddedPostgres POSTGRES = startPostgres();
    private static final Instant NOW = Instant.parse("2026-11-21T07:00:00Z");
    private static final UUID STATION_ID = UUID.randomUUID();
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final UUID FACTORY_ID = UUID.randomUUID();

    @Autowired
    private AccessService accessService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private com.tmc.accessevent.service.EvidenceReviewService reviews;
    @MockitoBean
    private com.tmc.accessevent.client.ReviewAccess reviewAccess;
    @MockitoBean
    private AccessPolicy accessPolicy;
    @MockitoBean
    private Clock clock;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    private static EmbeddedPostgres startPostgres() {
        try {
            return EmbeddedPostgres.start();
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @AfterAll
    static void stopPostgres() throws IOException {
        POSTGRES.close();
    }

    @BeforeEach
    void prepareDatabase() {
        jdbcTemplate.execute("TRUNCATE scan_requests, integration_outbox CASCADE");
        when(clock.instant()).thenReturn(NOW);
        when(accessPolicy.verify(any(), anyString(), anyString()))
                .thenReturn(verified(true, "ALLOWED", NOW.plusSeconds(60)));
    }

    private VerifiedAccess verified(boolean allowed, String reason, Instant expiresAt) {
        return new VerifiedAccess(EMPLOYEE_ID, FACTORY_ID, "ENTRY", UUID.randomUUID(), expiresAt, allowed, reason);
    }

    private ScanDecisionDTO scan() {
        return accessService.scan(UUID.randomUUID(), STATION_ID, "kiosk-1", "signed-qr");
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    @Test
    void approvedLateOpenedNeverCreatesPassageAndLatePassedRequiresValidOpenedEvidence() {
        var decision = scan();
        when(clock.instant()).thenReturn(NOW.plusSeconds(120));
        UUID opened = UUID.randomUUID();
        UUID passed = UUID.randomUUID();
        assertTrue(accessService.confirm(decision.commandId(), STATION_ID, opened, Outcome.OPENED, NOW.plusSeconds(1)).reviewRequired());
        assertTrue(accessService.confirm(decision.commandId(), STATION_ID, passed, Outcome.PASSED, NOW.plusSeconds(2)).reviewRequired());
        UUID actor = UUID.randomUUID();
        assertEquals("VALID_OPEN_EVIDENCE_REQUIRED", assertThrows(ResponseStatusException.class,
                () -> reviews.decide(FACTORY_ID, passed, UUID.randomUUID(), true, "checked", actor)).getReason());
        reviews.decide(FACTORY_ID, opened, UUID.randomUUID(), true, "controller journal confirmed", actor);
        assertEquals(0, jdbcTemplate.queryForObject("SELECT count(*) FROM access_events WHERE event_type = 'PASSAGE_CONFIRMED'", Integer.class));
        UUID reviewId = UUID.randomUUID();
        var result = reviews.decide(FACTORY_ID, passed, reviewId, true, "sensor evidence confirmed", actor);
        assertEquals("APPLIED", result.effect());
        assertEquals(result, reviews.decide(FACTORY_ID, passed, reviewId, true, "sensor evidence confirmed", actor));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT count(*) FROM access_events WHERE event_type = 'PASSAGE_CONFIRMED'", Integer.class));
        String body = jdbcTemplate.queryForObject("SELECT payload::text FROM outbox_events WHERE event_id = ?", String.class, result.eventId());
        assertTrue(body.contains(reviewId.toString()));
        assertTrue(body.contains(passed.toString()));
        assertEquals(2, count("integration_outbox"));
        assertEquals(0, reviews.pending(FACTORY_ID, null, 50, 0).total());
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbcTemplate.update("DELETE FROM command_confirmations"));
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbcTemplate.update("UPDATE access_evidence_reviews SET reason = 'changed'"));
    }

    @Test
    void reviewRejectsWrongFactorySelfApprovalChangedRetryAndOpenedAfterExpiry() {
        var decision = scan();
        when(clock.instant()).thenReturn(NOW.plusSeconds(120));
        UUID confirmation = UUID.randomUUID();
        accessService.confirm(decision.commandId(), STATION_ID, confirmation, Outcome.OPENED, NOW.plusSeconds(12));
        UUID actor = UUID.randomUUID();
        assertEquals(1, reviews.pending(FACTORY_ID, STATION_ID, 50, 0).total());
        assertEquals(0, reviews.pending(UUID.randomUUID(), null, 50, 0).total());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> reviews.decide(UUID.randomUUID(), confirmation, UUID.randomUUID(), false, "wrong site", actor)).getStatusCode().value());
        assertEquals("INDEPENDENT_REVIEWER_REQUIRED", assertThrows(ResponseStatusException.class,
                () -> reviews.decide(FACTORY_ID, confirmation, UUID.randomUUID(), false, "own evidence", EMPLOYEE_ID)).getReason());
        assertEquals("OPENED_AFTER_COMMAND_EXPIRED", assertThrows(ResponseStatusException.class,
                () -> reviews.decide(FACTORY_ID, confirmation, UUID.randomUUID(), true, "checked", actor)).getReason());
        UUID reviewId = UUID.randomUUID();
        assertEquals("REJECTED", reviews.decide(FACTORY_ID, confirmation, reviewId, false, "invalid controller time", actor).effect());
        assertThrows(ResponseStatusException.class,
                () -> reviews.decide(FACTORY_ID, confirmation, reviewId, true, "changed payload", actor));
        assertEquals(1, count("access_evidence_reviews"));
        assertEquals(1, count("access_events"));
    }

    @Test
    void concurrentReviewersCannotApplySameEvidenceTwice() throws Exception {
        var decision = scan();
        when(clock.instant()).thenReturn(NOW.plusSeconds(120));
        UUID confirmation = UUID.randomUUID();
        accessService.confirm(decision.commandId(), STATION_ID, confirmation, Outcome.OPENED, NOW.plusSeconds(1));
        try (var executor = Executors.newFixedThreadPool(2)) {
            var calls = java.util.stream.IntStream.range(0, 2).mapToObj(index -> executor.submit(() -> {
                try {
                    reviews.decide(FACTORY_ID, confirmation, UUID.randomUUID(), true, "independently verified", UUID.randomUUID());
                    return true;
                } catch (ResponseStatusException exception) {
                    assertEquals(409, exception.getStatusCode().value());
                    return false;
                }
            })).toList();
            int approved = 0;
            for (var call : calls) { if (call.get()) { approved++; } }
            assertEquals(1, approved);
        }
        assertEquals(1, count("access_evidence_reviews"));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT count(*) FROM access_events WHERE event_type = 'GATE_OPENED'", Integer.class));
    }

    @Test
    void reviewRoutesRequireScopeAndCurrentIdentitySiteRole() throws Exception {
        String path = "/factories/" + FACTORY_ID + "/reviews";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path).with(jwt()))
                .andExpect(status().isForbidden());
        var reviewer = jwt().authorities(new SimpleGrantedAuthority("SCOPE_access.review"));
        when(reviewAccess.require(any(), any(), anySet())).thenReturn(UUID.randomUUID());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path).with(reviewer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        when(reviewAccess.require(any(), any(), anySet())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path).with(reviewer))
                .andExpect(status().isForbidden());
    }

    @Test
    void legacyIncompleteEvidenceCanBeRejectedButCannotInventOutcome() {
        var decision = scan();
        UUID confirmation = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO command_confirmations (confirmation_id, command_id, fingerprint) VALUES (?, ?, ?)",
                confirmation, decision.commandId(), "0".repeat(64));
        UUID reviewer = UUID.randomUUID();
        assertNull(reviews.pending(FACTORY_ID, null, 50, 0).items().getFirst().outcome());
        assertEquals("LEGACY_EVIDENCE_INCOMPLETE", assertThrows(ResponseStatusException.class,
                () -> reviews.decide(FACTORY_ID, confirmation, UUID.randomUUID(), true, "unknown legacy data", reviewer)).getReason());
        assertEquals("REJECTED", reviews.decide(FACTORY_ID, confirmation, UUID.randomUUID(), false,
                "legacy evidence is incomplete", reviewer).effect());
        assertEquals(1, count("access_events"));
    }

    @Test
    void anotherLatePassageConfirmationDoesNotGenerateSecondPassageAfterReview() {
        var decision = scan();
        accessService.confirm(decision.commandId(), STATION_ID, UUID.randomUUID(), Outcome.OPENED, NOW.plusSeconds(1));
        accessService.confirm(decision.commandId(), STATION_ID, UUID.randomUUID(), Outcome.PASSED, NOW.plusSeconds(2));
        when(clock.instant()).thenReturn(NOW.plusSeconds(120));
        UUID confirmation = UUID.randomUUID();
        accessService.confirm(decision.commandId(), STATION_ID, confirmation, Outcome.PASSED, NOW.plusSeconds(3));
        var result = reviews.decide(FACTORY_ID, confirmation, UUID.randomUUID(), true,
                "same sensor passage already recorded", UUID.randomUUID());
        assertEquals("ALREADY_RECORDED", result.effect());
        assertNull(result.eventId());
        assertEquals(1, jdbcTemplate.queryForObject("SELECT count(*) FROM access_events WHERE event_type = 'PASSAGE_CONFIRMED'", Integer.class));
    }

    @Test
    void allowPersistsCommandEventAndOutboxWithoutMarkingAttendance() {
        ScanDecisionDTO decision = scan();
        assertTrue(decision.allowed());
        assertNotNull(decision.commandId());
        assertEquals(1, count("gate_commands"));
        assertEquals(1, count("access_events"));
        assertEquals(1, count("outbox_events"));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM access_events WHERE event_type = 'PASSAGE_CONFIRMED'", Integer.class));
    }

    @Test
    void retriesReturnSameDecisionEvenAfterQrExpiry() {
        UUID scanId = UUID.randomUUID();
        ScanDecisionDTO first = accessService.scan(scanId, STATION_ID, "kiosk-1", "signed-qr");
        when(clock.instant()).thenReturn(NOW.plusSeconds(120));
        assertEquals(first, accessService.scan(scanId, STATION_ID, "kiosk-1", "signed-qr"));
        assertEquals(1, count("gate_commands"));
        verify(accessPolicy, times(1)).verify(any(), anyString(), anyString());
    }

    @Test
    void retryPreservesTimestampPrecisionWithRealisticNanosecondClock() {
        when(clock.instant()).thenReturn(NOW.plusNanos(123456789));
        UUID scanId = UUID.randomUUID();
        ScanDecisionDTO first = accessService.scan(scanId, STATION_ID, "kiosk-1", "signed-qr");
        assertEquals(first, accessService.scan(scanId, STATION_ID, "kiosk-1", "signed-qr"));
        assertEquals(0, first.commandExpiresAt().getNano() % 1000);
    }

    @Test
    void differentPayloadWithSameScanIdIsRejected() {
        UUID scanId = UUID.randomUUID();
        accessService.scan(scanId, STATION_ID, "kiosk-1", "signed-qr");
        var exception = assertThrows(ResponseStatusException.class,
                () -> accessService.scan(scanId, STATION_ID, "kiosk-1", "other-qr"));
        assertEquals(409, exception.getStatusCode().value());
        assertEquals(1, count("scan_requests"));
    }

    @Test
    void concurrentDuplicateScanCreatesOneCommandAndEvent() throws Exception {
        UUID scanId = UUID.randomUUID();
        try (var executor = Executors.newFixedThreadPool(8)) {
            var tasks = new ArrayList<Callable<ScanDecisionDTO>>();
            for (int index = 0; index < 8; index++) {
                tasks.add(() -> accessService.scan(scanId, STATION_ID, "kiosk-1", "signed-qr"));
            }
            var results = executor.invokeAll(tasks);
            ScanDecisionDTO expected = results.getFirst().get();
            for (var result : results) {
                assertEquals(expected, result.get());
            }
        }
        assertEquals(1, count("gate_commands"));
        assertEquals(1, count("access_events"));
    }

    @Test
    void qrCannotBeUsedForAnotherScan() {
        assertTrue(scan().allowed());
        ScanDecisionDTO replay = scan();
        assertFalse(replay.allowed());
        assertEquals("QR_REPLAYED", replay.reason());
        assertEquals(1, count("gate_commands"));
        assertEquals(2, count("outbox_events"));
    }

    @Test
    void expiredQrDoesNotCreateCommand() {
        when(accessPolicy.verify(any(), anyString(), anyString())).thenReturn(verified(true, "ALLOWED", NOW));
        assertEquals("QR_EXPIRED", scan().reason());
        assertEquals(0, count("gate_commands"));
    }

    @Test
    void policyDenialIsDurableWithoutGateCommand() {
        when(accessPolicy.verify(any(), anyString(), anyString()))
                .thenReturn(verified(false, "SITE_ACCESS_DENIED", NOW.plusSeconds(60)));
        assertFalse(scan().allowed());
        assertEquals(0, count("gate_commands"));
        assertEquals(1, count("access_events"));
    }

    @Test
    void dependencyFailureRollsBackAndAllowsSameRequestToRetry() {
        when(accessPolicy.verify(any(), anyString(), anyString())).thenThrow(new IllegalStateException("unavailable"));
        UUID scanId = UUID.randomUUID();
        assertThrows(IllegalStateException.class,
                () -> accessService.scan(scanId, STATION_ID, "kiosk-1", "signed-qr"));
        assertEquals(0, count("scan_requests"));
        assertEquals(0, count("outbox_events"));
        doReturn(verified(true, "ALLOWED", NOW.plusSeconds(60)))
                .when(accessPolicy).verify(any(), anyString(), anyString());
        assertTrue(accessService.scan(scanId, STATION_ID, "kiosk-1", "signed-qr").allowed());
    }

    @Test
    void passageRequiresOpenedGateAndDuplicateConfirmationDoesNotAddEvents() {
        UUID commandId = scan().commandId();
        assertThrows(ResponseStatusException.class,
                () -> accessService.confirm(commandId, STATION_ID, UUID.randomUUID(), Outcome.PASSED, NOW));
        accessService.confirm(commandId, STATION_ID, UUID.randomUUID(), Outcome.OPENED, NOW);
        UUID confirmationId = UUID.randomUUID();
        assertEquals(Outcome.PASSED,
                accessService.confirm(commandId, STATION_ID, confirmationId, Outcome.PASSED, NOW).outcome());
        assertEquals(Outcome.PASSED,
                accessService.confirm(commandId, STATION_ID, confirmationId, Outcome.PASSED, NOW).outcome());
        assertEquals(3, count("access_events"));
        assertEquals(3, count("outbox_events"));
    }

    @Test
    void unknownOutcomeNeverCreatesAttendanceAndCanBeReconciled() {
        UUID commandId = scan().commandId();
        accessService.confirm(commandId, STATION_ID, UUID.randomUUID(), Outcome.UNKNOWN, NOW);
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM access_events WHERE event_type = 'PASSAGE_CONFIRMED'", Integer.class));
        accessService.confirm(commandId, STATION_ID, UUID.randomUUID(), Outcome.OPENED, NOW);
        accessService.confirm(commandId, STATION_ID, UUID.randomUUID(), Outcome.PASSED, NOW);
        assertEquals(4, count("access_events"));
    }

    @Test
    void otherStationCannotConfirmCommand() {
        UUID commandId = scan().commandId();
        var exception = assertThrows(ResponseStatusException.class, () -> accessService.confirm(
                commandId, UUID.randomUUID(), UUID.randomUUID(), Outcome.OPENED, NOW));
        assertEquals(403, exception.getStatusCode().value());
        assertEquals(0, count("command_confirmations"));
    }

    @Test
    void eventHistoryCannotBeUpdatedOrDeleted() {
        scan();
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbcTemplate.update("UPDATE access_events SET direction = 'EXIT'"));
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbcTemplate.update("DELETE FROM access_events"));
        assertEquals(1, count("access_events"));
    }

    @Test
    void lateOpenEvidenceIsStoredForReviewAndBackdatedPassageIsRejected() {
        UUID commandId = scan().commandId();
        when(clock.instant()).thenReturn(NOW.plusSeconds(20));
        var lateResult = accessService.confirm(commandId, STATION_ID, UUID.randomUUID(), Outcome.OPENED, NOW);
        assertTrue(lateResult.reviewRequired());
        assertEquals("PENDING", jdbcTemplate.queryForObject("SELECT outcome FROM gate_commands", String.class));
        when(clock.instant()).thenReturn(NOW.plusSeconds(1));
        accessService.confirm(commandId, STATION_ID, UUID.randomUUID(), Outcome.OPENED, NOW.plusSeconds(1));
        assertThrows(ResponseStatusException.class, () -> accessService.confirm(
                commandId, STATION_ID, UUID.randomUUID(), Outcome.PASSED, NOW));
        assertEquals(2, count("access_events"));
    }

    @Test
    void confirmationRetryReturnsOriginalResultAfterCommandProgresses() {
        UUID commandId = scan().commandId();
        UUID openedConfirmationId = UUID.randomUUID();
        var opened = accessService.confirm(commandId, STATION_ID, openedConfirmationId, Outcome.OPENED, NOW);
        accessService.confirm(commandId, STATION_ID, UUID.randomUUID(), Outcome.PASSED, NOW);
        when(clock.instant()).thenReturn(NOW.plusSeconds(120));
        assertEquals(opened, accessService.confirm(commandId, STATION_ID, openedConfirmationId, Outcome.OPENED, NOW));
        assertEquals(Outcome.OPENED, opened.outcome());
        assertEquals(3, count("access_events"));
        var exception = assertThrows(ResponseStatusException.class, () -> accessService.confirm(
                commandId, STATION_ID, openedConfirmationId, Outcome.PASSED, NOW));
        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void delayedPassageRemainsDurableWithoutAutomaticAttendance() {
        UUID commandId = scan().commandId();
        accessService.confirm(commandId, STATION_ID, UUID.randomUUID(), Outcome.OPENED, NOW);
        when(clock.instant()).thenReturn(NOW.plusSeconds(60));
        var latePassage = accessService.confirm(commandId, STATION_ID, UUID.randomUUID(), Outcome.PASSED, NOW.plusSeconds(1));
        assertTrue(latePassage.reviewRequired());
        assertEquals(2, count("command_confirmations"));
        assertEquals(2, count("access_events"));
        assertEquals("OPENED", jdbcTemplate.queryForObject("SELECT outcome FROM gate_commands", String.class));
    }

    @Test
    void concurrentConfirmationsStayIdempotent() throws Exception {
        UUID commandId = scan().commandId();
        UUID confirmationId = UUID.randomUUID();
        try (var executor = Executors.newFixedThreadPool(4)) {
            var tasks = new ArrayList<Callable<com.tmc.accessevent.dto.response.ConfirmationResultDTO>>();
            for (int index = 0; index < 4; index++) {
                tasks.add(() -> accessService.confirm(commandId, STATION_ID, confirmationId, Outcome.OPENED, NOW));
            }
            var results = executor.invokeAll(tasks);
            for (var result : results) {
                assertEquals(results.getFirst().get(), result.get());
            }
        }
        assertEquals(1, count("command_confirmations"));
        assertEquals(2, count("access_events"));
    }

    @Test
    void migrationUpgradesExistingV1EvidenceWithoutInventingOutcome() {
        String databaseUrl = POSTGRES.getJdbcUrl("postgres", "postgres");
        var v1 = org.flywaydb.core.Flyway.configure().dataSource(databaseUrl, "postgres", "postgres")
                .schemas("upgrade_test").defaultSchema("upgrade_test").target("1").load();
        v1.migrate();
        UUID scanId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        UUID confirmationId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO upgrade_test.scan_requests (scan_id, station_id, device_id, fingerprint)
                VALUES (?, ?, 'legacy-device', ?)
                """, scanId, STATION_ID, "0".repeat(64));
        jdbcTemplate.update("""
                INSERT INTO upgrade_test.gate_commands (command_id, scan_id, station_id, employee_id,
                    factory_id, direction, expires_at, outcome) VALUES (?, ?, ?, ?, ?, 'ENTRY', CURRENT_TIMESTAMP, 'OPENED')
                """, commandId, scanId, STATION_ID, EMPLOYEE_ID, FACTORY_ID);
        jdbcTemplate.update("""
                INSERT INTO upgrade_test.command_confirmations (confirmation_id, command_id, fingerprint)
                VALUES (?, ?, ?)
                """, confirmationId, commandId, "0".repeat(64));
        var upgraded = org.flywaydb.core.Flyway.configure().dataSource(databaseUrl, "postgres", "postgres")
                .schemas("upgrade_test").defaultSchema("upgrade_test").load();
        upgraded.migrate();
        assertEquals("3", upgraded.info().current().getVersion().toString());
        assertEquals(4, jdbcTemplate.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_schema = 'upgrade_test' AND table_name = 'command_confirmations'
                AND column_name IN ('reported_outcome', 'occurred_at', 'review_required', 'reason')
                """, Integer.class));
        assertTrue(jdbcTemplate.queryForObject(
                "SELECT review_required FROM upgrade_test.command_confirmations", Boolean.class));
        assertNull(jdbcTemplate.queryForObject(
                "SELECT reported_outcome FROM upgrade_test.command_confirmations", String.class));
    }

    @Test
    void outboxSurvivesBrokerFailureAndRetriesWithStableMessageId() {
        scan();
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        var publisher = new OutboxPublisher(jdbcTemplate, rabbitTemplate);
        var transaction = new TransactionTemplate(transactionManager);
        doThrow(new IllegalStateException("broker unavailable")).when(rabbitTemplate)
                .send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            try {
                publisher.publishPending();
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE published_at IS NULL", Integer.class));
        String eventId = jdbcTemplate.queryForObject("SELECT event_id::text FROM outbox_events", String.class);
        doAnswer(invocation -> {
            Message message = invocation.getArgument(2);
            CorrelationData correlation = invocation.getArgument(3);
            assertEquals(eventId, message.getMessageProperties().getMessageId());
            assertFalse(new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8).contains("signed-qr"));
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        transaction.executeWithoutResult(status -> {
            try {
                publisher.publishPending();
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        });
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE published_at IS NULL", Integer.class));
        verify(rabbitTemplate).setMandatory(true);
    }

    @Test
    void httpScanEnforcesAuthenticationScopeStationBindingAndValidation() throws Exception {
        String payload = """
                {"scanId":"%s","stationId":"%s","qrToken":"signed-qr"}
                """.formatted(UUID.randomUUID(), STATION_ID);
        mockMvc.perform(post("/scans").contentType("application/json").content(payload))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty()).andExpect(jsonPath("$.timestamp").isNotEmpty());
        mockMvc.perform(post("/scans").with(jwt()).contentType("application/json").content(payload))
                .andExpect(status().isForbidden());
        var otherStation = jwt().jwt(token -> token.subject("kiosk-2")
                .claim("station_id", UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("SCOPE_access.scan"));
        mockMvc.perform(post("/scans").with(otherStation).contentType("application/json").content(payload))
                .andExpect(status().isForbidden());
        var kiosk = jwt().jwt(token -> token.subject("kiosk-1").claim("station_id", STATION_ID.toString()))
                .authorities(new SimpleGrantedAuthority("SCOPE_access.scan"));
        mockMvc.perform(post("/scans").with(kiosk).contentType("application/json").content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.allowed").value(true)).andExpect(jsonPath("$.traceId").isNotEmpty());
        mockMvc.perform(post("/scans").with(kiosk).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray());
        assertEquals(1, count("scan_requests"));
    }
}
