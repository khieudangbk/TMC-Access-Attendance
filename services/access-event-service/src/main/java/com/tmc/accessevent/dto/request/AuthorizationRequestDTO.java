package com.tmc.accessevent.dto.request;

import java.util.Set;
import java.util.UUID;

public record AuthorizationRequestDTO(UUID employeeId, UUID factoryId, Set<String> roles) {
}
