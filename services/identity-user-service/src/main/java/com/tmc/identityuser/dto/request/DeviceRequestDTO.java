package com.tmc.identityuser.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DeviceRequestDTO(@NotBlank @Pattern(regexp = "kiosk-[a-zA-Z0-9-]{1,90}") String clientId,
        @NotBlank @Size(min = 12, max = 72) String secret, @NotNull UUID stationId) {
    @Override
    public String toString() {
        return "DeviceRequestDTO[clientId=" + clientId + ", secret=REDACTED]";
    }
}
