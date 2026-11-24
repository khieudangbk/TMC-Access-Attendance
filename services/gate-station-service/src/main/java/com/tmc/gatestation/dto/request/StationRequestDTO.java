package com.tmc.gatestation.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.tmc.gatestation.entity.Direction;

public record StationRequestDTO(@NotNull UUID factoryId,
        @NotBlank @Pattern(regexp = "[A-Z0-9-]{1,50}") String code,
        @NotBlank @Pattern(regexp = "kiosk-[a-zA-Z0-9-]{1,90}") String deviceId,
        @NotNull Direction direction, @NotBlank @Size(max = 150) String controllerReference) {
}
