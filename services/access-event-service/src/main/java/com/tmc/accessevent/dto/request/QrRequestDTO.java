package com.tmc.accessevent.dto.request;

import java.util.UUID;

public record QrRequestDTO(String token, UUID factoryId) {
    @Override
    public String toString() {
        return "QrRequestDTO[token=REDACTED]";
    }
}
