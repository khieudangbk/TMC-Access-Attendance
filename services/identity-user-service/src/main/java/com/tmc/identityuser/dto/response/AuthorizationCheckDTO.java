package com.tmc.identityuser.dto.response;

import java.util.UUID;

public record AuthorizationCheckDTO(UUID employeeId, UUID factoryId, boolean allowed) {
}
