package com.tmc.identityuser.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileRequestDTO(@NotBlank @Size(max = 200) String fullName) {
}
