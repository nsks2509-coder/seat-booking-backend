package dev.sriram.booking.web.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record EventCreateRequest(
        @NotBlank @Size(max = 160) String name,
        @NotNull @Future Instant startsAt,
        @Min(1) @Max(5000) int seatCount) {}
