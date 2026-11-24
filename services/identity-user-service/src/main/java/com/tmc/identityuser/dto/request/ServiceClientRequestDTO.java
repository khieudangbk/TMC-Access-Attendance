package com.tmc.identityuser.dto.request;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ServiceClientRequestDTO(@NotBlank @Pattern(regexp = "service-[a-zA-Z0-9-]{1,80}") String clientId,
        @NotBlank @Size(min = 12, max = 72) String secret, @NotEmpty Set<@NotBlank String> scopes) {
    @Override
    public String toString() {
        return "ServiceClientRequestDTO[clientId=" + clientId + ", secret=REDACTED]";
    }
}
