package com.bookify.backend.availability.service;

import com.bookify.backend.availability.dto.*;
import com.bookify.backend.availability.model.ResourceScheduleException;
import com.bookify.backend.availability.model.ResourceScheduleRule;
import com.bookify.backend.availability.model.ScheduleExceptionType;
import com.bookify.backend.availability.model.ScheduleRuleType;
import com.bookify.backend.availability.repository.ResourceScheduleExceptionRepository;
import com.bookify.backend.availability.repository.ResourceScheduleRuleRepository;
import com.bookify.backend.common.exception.BadRequestException;
import com.bookify.backend.common.exception.ResourceNotFoundException;
import com.bookify.backend.resource.model.BookableResource;
import com.bookify.backend.resource.repository.BookableResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ResourceScheduleService {

    private final ResourceScheduleRuleRepository ruleRepository;
    private final ResourceScheduleExceptionRepository exceptionRepository;
    private final BookableResourceRepository resourceRepository;

    public ResourceScheduleService(
            ResourceScheduleRuleRepository ruleRepository,
            ResourceScheduleExceptionRepository exceptionRepository,
            BookableResourceRepository resourceRepository
    ) {
        this.ruleRepository = ruleRepository;
        this.exceptionRepository = exceptionRepository;
        this.resourceRepository = resourceRepository;
    }

    @Transactional
    public ResourceScheduleResponse replaceSchedule(
            Long businessId,
            Long locationId,
            Long resourceId,
            List<ScheduleRuleRequest> requests
    ) {
        BookableResource resource = requireResource(businessId, locationId, resourceId);
        validateRules(requests);

        ruleRepository.deleteByResourceId(resourceId);
        ruleRepository.flush();
        ruleRepository.saveAll(requests.stream()
                .map(request -> new ResourceScheduleRule(
                        resource.getBusiness(),
                        resource,
                        request.dayOfWeek(),
                        request.ruleType(),
                        request.startTime(),
                        request.endTime()
                ))
                .toList());

        return scheduleResponse(resource);
    }

    @Transactional(readOnly = true)
    public ResourceScheduleResponse findSchedule(
            Long businessId,
            Long locationId,
            Long resourceId
    ) {
        return scheduleResponse(requireResource(businessId, locationId, resourceId));
    }

    @Transactional
    public ScheduleExceptionResponse upsertException(
            Long businessId,
            Long locationId,
            Long resourceId,
            LocalDate date,
            UpsertScheduleExceptionRequest request
    ) {
        BookableResource resource = requireResource(businessId, locationId, resourceId);
        validateException(request);

        ResourceScheduleException exception = exceptionRepository
                .findByBusinessIdAndLocationIdAndResourceIdAndExceptionDate(
                        businessId, locationId, resourceId, date
                )
                .orElseGet(() -> new ResourceScheduleException(
                        resource.getBusiness(), resource, date
                ));
        exception.update(
                request.exceptionType(),
                request.startTime(),
                request.endTime(),
                trimToNull(request.reason())
        );
        return ScheduleExceptionResponse.from(exceptionRepository.saveAndFlush(exception));
    }

    @Transactional(readOnly = true)
    public List<ScheduleExceptionResponse> findExceptions(
            Long businessId,
            Long locationId,
            Long resourceId,
            LocalDate from,
            LocalDate to
    ) {
        requireResource(businessId, locationId, resourceId);
        if (from.isAfter(to)) {
            throw new BadRequestException("Exception range start must not be after end");
        }
        if (ChronoUnit.DAYS.between(from, to) > 366) {
            throw new BadRequestException("Exception range cannot exceed 366 days");
        }
        return exceptionRepository
                .findByBusinessIdAndLocationIdAndResourceIdAndExceptionDateBetweenOrderByExceptionDateAsc(
                        businessId, locationId, resourceId, from, to
                )
                .stream()
                .map(ScheduleExceptionResponse::from)
                .toList();
    }

    @Transactional
    public void deleteException(
            Long businessId,
            Long locationId,
            Long resourceId,
            LocalDate date
    ) {
        ResourceScheduleException exception = exceptionRepository
                .findByBusinessIdAndLocationIdAndResourceIdAndExceptionDate(
                        businessId, locationId, resourceId, date
                )
                .orElseThrow(() -> new ResourceNotFoundException("Schedule exception not found"));
        exceptionRepository.delete(exception);
    }

    private ResourceScheduleResponse scheduleResponse(BookableResource resource) {
        List<ScheduleRuleResponse> rules = ruleRepository
                .findByBusinessIdAndLocationIdAndResourceIdOrderByDayOfWeekAscStartTimeAsc(
                        resource.getBusiness().getId(),
                        resource.getLocation().getId(),
                        resource.getId()
                )
                .stream()
                .sorted(
                        Comparator
                                .comparingInt((ResourceScheduleRule rule) ->
                                        rule.getDayOfWeek().getValue())
                                .thenComparing(ResourceScheduleRule::getStartTime)
                                .thenComparing(ResourceScheduleRule::getRuleType)
                )
                .map(ScheduleRuleResponse::from)
                .toList();
        return new ResourceScheduleResponse(
                resource.getBusiness().getId(),
                resource.getLocation().getId(),
                resource.getId(),
                resource.getLocation().getTimezone(),
                rules
        );
    }

    private void validateRules(List<ScheduleRuleRequest> requests) {
        for (ScheduleRuleRequest request : requests) {
            if (!request.startTime().isBefore(request.endTime())) {
                throw new BadRequestException("Schedule rule start must be before end");
            }
        }

        Map<DayOfWeek, List<ScheduleRuleRequest>> byDay = requests.stream()
                .collect(Collectors.groupingBy(ScheduleRuleRequest::dayOfWeek));
        byDay.forEach((day, rules) -> {
            List<ScheduleRuleRequest> available = filter(rules, ScheduleRuleType.AVAILABLE);
            List<ScheduleRuleRequest> breaks = filter(rules, ScheduleRuleType.BREAK);
            requireNoOverlap(available, "Available intervals cannot overlap");
            requireNoOverlap(breaks, "Break intervals cannot overlap");
            for (ScheduleRuleRequest breakRule : breaks) {
                boolean contained = available.stream().anyMatch(interval ->
                        !breakRule.startTime().isBefore(interval.startTime())
                                && !breakRule.endTime().isAfter(interval.endTime())
                );
                if (!contained) {
                    throw new BadRequestException(
                            "Each break must be contained in an available interval"
                    );
                }
            }
        });
    }

    private List<ScheduleRuleRequest> filter(
            List<ScheduleRuleRequest> rules,
            ScheduleRuleType type
    ) {
        return rules.stream()
                .filter(rule -> rule.ruleType() == type)
                .sorted(Comparator.comparing(ScheduleRuleRequest::startTime))
                .toList();
    }

    private void requireNoOverlap(List<ScheduleRuleRequest> rules, String message) {
        List<ScheduleRuleRequest> ordered = new ArrayList<>(rules);
        for (int index = 1; index < ordered.size(); index++) {
            if (ordered.get(index).startTime().isBefore(ordered.get(index - 1).endTime())) {
                throw new BadRequestException(message);
            }
        }
    }

    private void validateException(UpsertScheduleExceptionRequest request) {
        if (request.exceptionType() == ScheduleExceptionType.CLOSED) {
            if (request.startTime() != null || request.endTime() != null) {
                throw new BadRequestException("Closed exceptions must not include times");
            }
            return;
        }
        if (request.startTime() == null || request.endTime() == null
                || !request.startTime().isBefore(request.endTime())) {
            throw new BadRequestException(
                    "Custom-hours exceptions require a valid start and end time"
            );
        }
    }

    private BookableResource requireResource(Long businessId, Long locationId, Long resourceId) {
        return resourceRepository.findByIdAndBusinessIdAndLocationId(
                        resourceId, businessId, locationId
                )
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
