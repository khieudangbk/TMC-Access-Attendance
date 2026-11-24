package com.tmc.api;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiResponse(String code, String message, Object data, Instant timestamp,
        String traceId, @JsonInclude(JsonInclude.Include.NON_NULL) List<FieldError> errors) {

    public record FieldError(String field, String message) {
    }
}
