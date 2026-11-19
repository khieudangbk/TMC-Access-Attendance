package com.tmc.qr.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VerifyRequestDTO(@NotBlank @Size(max = 8192) String token, @NotNull UUID factoryId) {
    @Override
    public String toString() {
        return "VerifyRequestDTO[token=REDACTED]";
    }
}
