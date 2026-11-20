package com.tmc.aacs.central.shared.error;

import java.time.Instant;
import java.util.List;

public record ApiError(
        String code,
        String message,
        List<FieldViolation> details,
        Instant timestamp
) {

    public record FieldViolation(String field, String message) {
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of(), Instant.now());
    }
}
