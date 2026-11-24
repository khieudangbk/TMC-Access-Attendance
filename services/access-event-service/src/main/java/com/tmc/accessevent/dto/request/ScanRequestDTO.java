package com.tmc.accessevent.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScanRequestDTO(@NotNull UUID scanId, @NotNull UUID stationId,
        @NotBlank @Size(max = 8192) String qrToken) {
    @Override
    public String toString() {
        return "ScanRequestDTO[scanId=" + scanId + ", stationId=" + stationId + ", qrToken=REDACTED]";
    }
}
