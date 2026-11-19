package com.tmc.accessevent.dto.request;

import java.util.UUID;

public record PermissionRequestDTO(UUID employeeId, UUID factoryId) {
}
