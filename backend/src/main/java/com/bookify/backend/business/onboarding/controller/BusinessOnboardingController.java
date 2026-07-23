package com.bookify.backend.business.onboarding.controller;

import com.bookify.backend.business.onboarding.dto.BusinessOnboardingResponse;
import com.bookify.backend.business.onboarding.dto.CreateBusinessRequest;
import com.bookify.backend.business.onboarding.dto.MyBusinessResponse;
import com.bookify.backend.business.onboarding.service.BusinessOnboardingService;
import com.bookify.backend.common.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class BusinessOnboardingController {

    private final BusinessOnboardingService onboardingService;

    public BusinessOnboardingController(BusinessOnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/businesses")
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessOnboardingResponse onboard(@Valid @RequestBody CreateBusinessRequest request) {
        return onboardingService.onboard(SecurityUtils.getCurrentUserEmail(), request);
    }

    @GetMapping("/me/businesses")
    public List<MyBusinessResponse> findMyBusinesses() {
        return onboardingService.findMyBusinesses(SecurityUtils.getCurrentUserEmail());
    }
}
