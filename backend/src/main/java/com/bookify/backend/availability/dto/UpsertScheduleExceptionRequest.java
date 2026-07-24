package com.bookify.backend.availability.dto;

import com.bookify.backend.availability.model.ScheduleExceptionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record UpsertScheduleExceptionRequest(
        @NotNull ScheduleExceptionType exceptionType,
        LocalTime startTime,
        LocalTime endTime,
        @Size(max = 250) String reason
) {
}
