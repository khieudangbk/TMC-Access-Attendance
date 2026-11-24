package com.tmc.accessevent.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.tmc.accessevent.dto.response.ScanDecisionDTO;
import com.tmc.accessevent.entity.VerifiedAccess;
import com.tmc.accessevent.repository.AccessStore;
import com.tmc.accessevent.service.impl.AccessServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AccessServiceTests {

    private final AccessStore accessStore = mock(AccessStore.class);
    private final AccessPolicy accessPolicy = mock(AccessPolicy.class);
    private final Instant now = Instant.parse("2026-11-21T07:00:00Z");
    private final AccessService accessService = new AccessServiceImpl(accessStore, accessPolicy,
            Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void duplicateDoesNotCallExternalPolicyAgain() {
        UUID scanId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        var saved = new ScanDecisionDTO(scanId, stationId, null, null, null, false, "DENIED", null, null);
        when(accessStore.existingDecision(any(), anyString())).thenReturn(saved);
        assertEquals(saved, accessService.scan(scanId, stationId, "device-1", "qr-token"));
        verifyNoInteractions(accessPolicy);
    }

    @Test
    void expiryIsCheckedBeforeRedeemingQr() {
        when(accessStore.claimScan(any(), any(), anyString(), anyString())).thenReturn(true);
        when(accessPolicy.verify(any(), anyString(), anyString())).thenReturn(
                new VerifiedAccess(UUID.randomUUID(), UUID.randomUUID(), "ENTRY", UUID.randomUUID(), now, true, "ALLOWED"));
        var decision = accessService.scan(UUID.randomUUID(), UUID.randomUUID(), "device-1", "qr-token");
        assertFalse(decision.allowed());
        assertEquals("QR_EXPIRED", decision.reason());
        verify(accessStore, never()).redeemQr(any(), any(), any());
    }
}
