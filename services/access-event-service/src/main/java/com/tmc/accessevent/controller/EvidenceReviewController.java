package com.tmc.accessevent.controller;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tmc.accessevent.client.ReviewAccess;
import com.tmc.accessevent.dto.response.EvidenceDTO;
import com.tmc.accessevent.dto.response.ReviewDTO;
import com.tmc.accessevent.dto.request.ReviewDecisionRequestDTO;
import com.tmc.accessevent.dto.response.ReviewDetailDTO;
import com.tmc.accessevent.service.EvidenceReviewService;
import com.tmc.api.PageData;

@RestController
@RequestMapping("/factories/{factoryId}/reviews")
public class EvidenceReviewController {
    private final EvidenceReviewService service;
    private final ReviewAccess access;

    public EvidenceReviewController(EvidenceReviewService service, ReviewAccess access) {
        this.service = service;
        this.access = access;
    }

    @GetMapping
    PageData<EvidenceDTO> pending(@PathVariable UUID factoryId, @RequestParam(required = false) UUID stationId,
            @RequestParam(defaultValue = "50") int limit, @RequestParam(defaultValue = "0") int offset,
            @AuthenticationPrincipal Jwt jwt) {
        access.require(jwt, factoryId, Set.of("HR", "SECURITY"));
        return service.pending(factoryId, stationId, limit, offset);
    }

    @PostMapping("/{confirmationId}/decisions")
    ReviewDTO decide(@PathVariable UUID factoryId, @PathVariable UUID confirmationId,
            @Valid @RequestBody ReviewDecisionRequestDTO request, @AuthenticationPrincipal Jwt jwt) {
        UUID actor = access.require(jwt, factoryId, Set.of("HR", "SECURITY"));
        return service.decide(factoryId, confirmationId, request.decisionId(), request.approved(), request.reason(), actor);
    }

    @GetMapping("/{confirmationId}")
    ReviewDetailDTO detail(@PathVariable UUID factoryId, @PathVariable UUID confirmationId,
            @AuthenticationPrincipal Jwt jwt) {
        access.require(jwt, factoryId, Set.of("HR", "SECURITY"));
        return service.detail(factoryId, confirmationId);
    }
}
