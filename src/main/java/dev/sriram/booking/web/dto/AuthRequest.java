package dev.sriram.booking.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
        @NotBlank @Size(min = 3, max = 80) String username,
        @NotBlank @Size(min = 12, max = 72) String password) {}
