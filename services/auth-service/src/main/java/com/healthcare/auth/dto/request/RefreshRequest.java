package com.healthcare.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(

        @NotBlank
        @Size(min = 16, max = 512)
        String refreshToken
) { }
