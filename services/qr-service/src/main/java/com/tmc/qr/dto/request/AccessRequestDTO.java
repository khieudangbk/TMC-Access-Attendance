package com.tmc.qr.dto.request;

import java.util.UUID;

public record AccessRequestDTO(UUID employeeId, UUID factoryId) {
}
