package com.tmc.identityuser.dto.response;

import java.util.UUID;

public record AccessCheckDTO(UUID employeeId, UUID factoryId, boolean allowed, String reason) {
}
