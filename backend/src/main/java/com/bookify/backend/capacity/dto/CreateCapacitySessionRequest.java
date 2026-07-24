package com.bookify.backend.capacity.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateCapacitySessionRequest(
        @NotNull Long resourceId,
        @NotNull @Future Instant startsAt,
        @NotNull @Min(1) @Max(100000) Integer capacityTotal
) {}
