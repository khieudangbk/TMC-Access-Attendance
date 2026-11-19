package com.tmc.gatestation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CheckRequestDTO(@NotBlank @Size(max = 100) String deviceId) {
}
