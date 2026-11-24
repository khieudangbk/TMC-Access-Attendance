package com.tmc.qr.dto.response;

import java.util.UUID;

public record AccessResultDTO(UUID employeeId, UUID factoryId, Boolean allowed, String reason) {
}
