package com.tmc.accessevent.service;

import java.util.UUID;

import com.tmc.accessevent.dto.response.EvidenceDTO;
import com.tmc.accessevent.dto.response.ReviewDTO;
import com.tmc.accessevent.dto.response.ReviewDetailDTO;
import com.tmc.api.PageData;

public interface EvidenceReviewService {

    PageData<EvidenceDTO> pending(UUID factoryId, UUID stationId, int limit, int offset);

    ReviewDetailDTO detail(UUID factoryId, UUID confirmationId);

    ReviewDTO decide(UUID factoryId, UUID confirmationId, UUID decisionId, boolean approved, String reason, UUID actor);
}
