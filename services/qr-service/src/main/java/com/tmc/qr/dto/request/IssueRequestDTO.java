package com.tmc.qr.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record IssueRequestDTO(@NotNull UUID factoryId) {
}
