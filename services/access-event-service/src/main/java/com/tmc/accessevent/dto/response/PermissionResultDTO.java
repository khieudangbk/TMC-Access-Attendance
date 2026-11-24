package com.tmc.accessevent.dto.response;

import java.util.UUID;

public record PermissionResultDTO(UUID employeeId, UUID factoryId, Boolean allowed, String reason) {
}
