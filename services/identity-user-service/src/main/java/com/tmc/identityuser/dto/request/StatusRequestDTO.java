package com.tmc.identityuser.dto.request;

import jakarta.validation.constraints.NotNull;

public record StatusRequestDTO(@NotNull Boolean active) {
}
