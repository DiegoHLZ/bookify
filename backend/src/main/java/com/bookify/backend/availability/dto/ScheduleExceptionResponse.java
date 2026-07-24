package com.bookify.backend.availability.dto;

import com.bookify.backend.availability.model.ResourceScheduleException;
import com.bookify.backend.availability.model.ScheduleExceptionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ScheduleExceptionResponse(
        Long id,
        Long resourceId,
        LocalDate exceptionDate,
        ScheduleExceptionType exceptionType,
        LocalTime startTime,
        LocalTime endTime,
        String reason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ScheduleExceptionResponse from(ResourceScheduleException exception) {
        return new ScheduleExceptionResponse(
                exception.getId(),
                exception.getResource().getId(),
                exception.getExceptionDate(),
                exception.getExceptionType(),
                exception.getStartTime(),
                exception.getEndTime(),
                exception.getReason(),
                exception.getCreatedAt(),
                exception.getUpdatedAt()
        );
    }
}
