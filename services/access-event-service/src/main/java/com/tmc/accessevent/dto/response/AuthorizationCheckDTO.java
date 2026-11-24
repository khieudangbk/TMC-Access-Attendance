package com.tmc.accessevent.dto.response;

import java.util.UUID;

public record AuthorizationCheckDTO(UUID employeeId, UUID factoryId, Boolean allowed) {
}
