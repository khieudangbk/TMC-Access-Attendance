package com.tmc.qr;

import java.util.UUID;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

class QrMigrationTests {
    @Test
    void upgradeBackfillsLegacyRevocationWithoutInventingFactoryAndProtectsEvidence() throws Exception {
        try (var postgres = EmbeddedPostgres.start()) {
            var source = postgres.getPostgresDatabase();
            var database = new JdbcTemplate(source);
            Flyway.configure().dataSource(source).target("1").load().migrate();
            UUID employeeId = UUID.randomUUID();
            UUID credentialId = UUID.randomUUID();
            UUID factoryId = UUID.randomUUID();
            UUID issuedEvent = UUID.randomUUID();
            UUID revokedEvent = UUID.randomUUID();
            database.update("""
                    INSERT INTO qr_credentials VALUES (?, ?, ?, CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP + interval '1 minute', 'legacy-key', NULL)
                    """, credentialId, employeeId, factoryId);
            database.update("INSERT INTO qr_audit_events (event_id, employee_id, qr_id, event_type) VALUES (?, ?, ?, 'QR_ISSUED')",
                    issuedEvent, employeeId, credentialId);
            database.update("INSERT INTO qr_audit_events (event_id, employee_id, event_type) VALUES (?, ?, 'QR_REVOKED')",
                    revokedEvent, employeeId);
            Flyway.configure().dataSource(source).load().migrate();
            assertEquals(2, database.queryForObject("SELECT count(*) FROM integration_outbox", Integer.class));
            assertEquals(employeeId.toString(), database.queryForObject(
                    "SELECT payload->>'entityId' FROM integration_outbox WHERE event_id = ?", String.class, revokedEvent));
            assertNull(database.queryForObject("SELECT payload->>'factoryId' FROM integration_outbox WHERE event_id = ?",
                    String.class, revokedEvent));
            assertEquals(factoryId.toString(), database.queryForObject(
                    "SELECT payload->>'factoryId' FROM integration_outbox WHERE event_id = ?", String.class, issuedEvent));
            assertThrows(org.springframework.dao.DataAccessException.class,
                    () -> database.update("DELETE FROM qr_audit_events"));
            assertThrows(org.springframework.dao.DataAccessException.class,
                    () -> database.update("UPDATE integration_outbox SET payload = '{}'::jsonb"));
            assertEquals(1, database.update("UPDATE integration_outbox SET published_at = CURRENT_TIMESTAMP WHERE event_id = ?", issuedEvent));
            Flyway.configure().dataSource(source).load().migrate();
            assertEquals(2, database.queryForObject("SELECT count(*) FROM integration_outbox", Integer.class));
        }
    }
}
