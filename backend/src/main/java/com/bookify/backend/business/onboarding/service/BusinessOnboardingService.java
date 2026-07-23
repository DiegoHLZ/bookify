package com.bookify.backend.business.onboarding.service;

import com.bookify.backend.business.model.Business;
import com.bookify.backend.business.category.repository.BusinessCategoryRepository;
import com.bookify.backend.business.onboarding.dto.BusinessOnboardingResponse;
import com.bookify.backend.business.onboarding.dto.CreateBusinessRequest;
import com.bookify.backend.business.onboarding.dto.CreateLocationRequest;
import com.bookify.backend.business.onboarding.dto.MyBusinessResponse;
import com.bookify.backend.business.repository.BusinessRepository;
import com.bookify.backend.common.exception.BadRequestException;
import com.bookify.backend.common.exception.ResourceNotFoundException;
import com.bookify.backend.location.model.BusinessLocation;
import com.bookify.backend.location.repository.BusinessLocationRepository;
import com.bookify.backend.tenancy.model.BusinessMembership;
import com.bookify.backend.tenancy.model.MembershipRole;
import com.bookify.backend.tenancy.repository.BusinessMembershipRepository;
import com.bookify.backend.user.model.User;
import com.bookify.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Service
public class BusinessOnboardingService {

    private final BusinessRepository businessRepository;
    private final BusinessLocationRepository locationRepository;
    private final BusinessMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final BusinessCategoryRepository categoryRepository;

    public BusinessOnboardingService(
            BusinessRepository businessRepository,
            BusinessLocationRepository locationRepository,
            BusinessMembershipRepository membershipRepository,
            UserRepository userRepository,
            BusinessCategoryRepository categoryRepository
    ) {
        this.businessRepository = businessRepository;
        this.locationRepository = locationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public BusinessOnboardingResponse onboard(String authenticatedEmail, CreateBusinessRequest request) {
        String slug = request.slug().trim().toLowerCase(Locale.ROOT);
        if (businessRepository.existsBySlugIgnoreCase(slug)) {
            throw new BadRequestException("Business slug is already in use");
        }

        String categoryCode = request.categoryCode().trim().toUpperCase(Locale.ROOT);
        if (!categoryRepository.existsByCodeAndActiveTrue(categoryCode)) {
            throw new BadRequestException("Business category is invalid or inactive");
        }

        validateTimezone(request.location().timezone());

        User owner = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Active user not found"));

        Business business = new Business();
        business.setName(request.name().trim());
        business.setSlug(slug);
        business.setCategoryCode(categoryCode);
        business.setDescription(trimToNull(request.description()));
        business.setPhone(trimToNull(request.phone()));
        business.setEmail(normalizeEmail(request.email()));
        business.setAddress(request.location().address().trim());
        business.setActive(true);
        businessRepository.save(business);

        CreateLocationRequest locationRequest = request.location();
        BusinessLocation location = new BusinessLocation(
                business,
                locationRequest.name().trim(),
                locationRequest.address().trim(),
                locationRequest.city().trim(),
                locationRequest.countryCode().trim().toUpperCase(Locale.ROOT),
                locationRequest.timezone().trim(),
                locationRequest.latitude(),
                locationRequest.longitude()
        );
        locationRepository.save(location);

        BusinessMembership membership = new BusinessMembership(business, owner, MembershipRole.OWNER);
        membershipRepository.save(membership);

        return BusinessOnboardingResponse.from(business, membership.getRole(), location);
    }

    @Transactional(readOnly = true)
    public List<MyBusinessResponse> findMyBusinesses(String authenticatedEmail) {
        return membershipRepository.findActiveBusinessesByUserEmail(authenticatedEmail)
                .stream()
                .map(MyBusinessResponse::from)
                .toList();
    }

    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone.trim());
        } catch (DateTimeException exception) {
            throw new BadRequestException("Location timezone must be a valid IANA timezone");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
