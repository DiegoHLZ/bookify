package com.bookify.backend.tenancy.service;

import com.bookify.backend.business.model.Business;
import com.bookify.backend.business.repository.BusinessRepository;
import com.bookify.backend.common.exception.BadRequestException;
import com.bookify.backend.common.exception.ResourceNotFoundException;
import com.bookify.backend.tenancy.dto.*;
import com.bookify.backend.tenancy.model.*;
import com.bookify.backend.tenancy.repository.BusinessInvitationRepository;
import com.bookify.backend.tenancy.repository.BusinessMembershipRepository;
import com.bookify.backend.user.model.User;
import com.bookify.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class BusinessTeamService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final BusinessMembershipRepository membershipRepository;
    private final BusinessInvitationRepository invitationRepository;
    private final BusinessAccessService accessService;
    private final long invitationExpirationHours;

    public BusinessTeamService(
            BusinessRepository businessRepository,
            UserRepository userRepository,
            BusinessMembershipRepository membershipRepository,
            BusinessInvitationRepository invitationRepository,
            BusinessAccessService accessService,
            @Value("${bookify.invitations.expiration-hours:72}")
            long invitationExpirationHours
    ) {
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.invitationRepository = invitationRepository;
        this.accessService = accessService;
        if (invitationExpirationHours < 1 || invitationExpirationHours > 720) {
            throw new IllegalArgumentException(
                    "Invitation expiration must be between 1 and 720 hours"
            );
        }
        this.invitationExpirationHours = invitationExpirationHours;
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(Long businessId, String actorEmail) {
        requireMemberViewer(businessId, actorEmail);
        return membershipRepository.findMembers(businessId).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MembershipPermissionsResponse permissions(Long businessId, String actorEmail) {
        MembershipRole role = accessService.requireRole(businessId, actorEmail);
        return new MembershipPermissionsResponse(
                businessId, role, Set.copyOf(accessService.permissionsFor(role))
        );
    }

    @Transactional
    public CreatedInvitationResponse invite(
            Long businessId,
            CreateInvitationRequest request,
            String actorEmail
    ) {
        Business business = lockBusiness(businessId);
        MembershipRole actorRole = accessService.requireRole(businessId, actorEmail);
        requireCanInvite(actorRole, request.role());
        User actor = requireActiveUser(actorEmail);
        String email = normalizeEmail(request.email());
        if (membershipRepository.existsByBusinessIdAndUserEmailAndActiveTrue(
                businessId, email
        )) {
            throw new BadRequestException("User is already an active business member");
        }

        invitationRepository.findByBusinessIdAndEmailIgnoreCaseAndStatus(
                businessId, email, InvitationStatus.PENDING
        ).ifPresent(existing -> {
            if (existing.getExpiresAt().isAfter(Instant.now())) {
                throw new BadRequestException("A pending invitation already exists");
            }
            existing.expire();
            invitationRepository.saveAndFlush(existing);
        });

        String token = newToken();
        BusinessInvitation invitation = new BusinessInvitation(
                business,
                email,
                request.role(),
                hashToken(token),
                Instant.now().plus(invitationExpirationHours, ChronoUnit.HOURS),
                actor
        );
        try {
            invitationRepository.saveAndFlush(invitation);
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException("A pending invitation already exists");
        }
        return new CreatedInvitationResponse(toInvitationResponse(invitation), token);
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> listInvitations(Long businessId, String actorEmail) {
        requireMemberViewer(businessId, actorEmail);
        return invitationRepository.findByBusinessIdOrderByCreatedAtDesc(businessId)
                .stream()
                .map(this::toInvitationResponse)
                .toList();
    }

    @Transactional
    public InvitationResponse revokeInvitation(
            Long businessId,
            Long invitationId,
            String actorEmail
    ) {
        lockBusiness(businessId);
        MembershipRole actorRole = accessService.requireRole(businessId, actorEmail);
        BusinessInvitation invitation = invitationRepository
                .findForUpdateByIdAndBusinessId(invitationId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        requireCanManageRole(actorRole, invitation.getRole());
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("Only pending invitations can be revoked");
        }
        invitation.revoke();
        return toInvitationResponse(invitationRepository.save(invitation));
    }

    @Transactional
    public MemberResponse acceptInvitation(String rawToken, String actorEmail) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 200) {
            throw new BadRequestException("Invitation token is invalid");
        }
        String tokenHash = hashToken(rawToken);
        BusinessInvitation preview = invitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invitation token is invalid"));
        lockBusiness(preview.getBusiness().getId());
        BusinessInvitation invitation = invitationRepository.findForUpdateByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invitation token is invalid"));
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("Invitation is no longer pending");
        }
        if (!invitation.getExpiresAt().isAfter(Instant.now())) {
            invitation.expire();
            invitationRepository.save(invitation);
            throw new BadRequestException("Invitation has expired");
        }
        String normalizedActorEmail = normalizeEmail(actorEmail);
        if (!invitation.getEmail().equalsIgnoreCase(normalizedActorEmail)) {
            throw new AccessDeniedException("Invitation belongs to another user");
        }
        User user = requireActiveUser(normalizedActorEmail);
        BusinessMembership membership = membershipRepository
                .findByBusinessIdAndUserEmailIgnoreCase(
                        invitation.getBusiness().getId(), normalizedActorEmail
                )
                .orElseGet(() -> new BusinessMembership(
                        invitation.getBusiness(), user, invitation.getRole()
                ));
        membership.changeRole(invitation.getRole());
        membership.setActive(true);
        membershipRepository.save(membership);
        invitation.accept(user, Instant.now());
        invitationRepository.save(invitation);
        return toMemberResponse(membership);
    }

    @Transactional
    public MemberResponse changeRole(
            Long businessId,
            Long membershipId,
            MembershipRole newRole,
            String actorEmail
    ) {
        lockBusiness(businessId);
        MembershipRole actorRole = accessService.requireRole(businessId, actorEmail);
        BusinessMembership target = requireMembership(businessId, membershipId);
        requireCanManageRole(actorRole, target.getRole());
        requireCanInvite(actorRole, newRole);
        if (target.isActive()
                && target.getRole() == MembershipRole.OWNER
                && newRole != MembershipRole.OWNER) {
            requireAnotherOwner(businessId);
        }
        target.changeRole(newRole);
        return toMemberResponse(membershipRepository.save(target));
    }

    @Transactional
    public MemberResponse changeStatus(
            Long businessId,
            Long membershipId,
            boolean active,
            String actorEmail
    ) {
        lockBusiness(businessId);
        MembershipRole actorRole = accessService.requireRole(businessId, actorEmail);
        BusinessMembership target = requireMembership(businessId, membershipId);
        requireCanManageRole(actorRole, target.getRole());
        if (!active && target.isActive() && target.getRole() == MembershipRole.OWNER) {
            requireAnotherOwner(businessId);
        }
        target.setActive(active);
        return toMemberResponse(membershipRepository.save(target));
    }

    private void requireMemberViewer(Long businessId, String actorEmail) {
        MembershipRole role = accessService.requireRole(businessId, actorEmail);
        if (!accessService.permissionsFor(role).contains(MembershipPermission.VIEW_MEMBERS)) {
            throw new AccessDeniedException("Member administration access required");
        }
    }

    private void requireCanInvite(MembershipRole actorRole, MembershipRole assignedRole) {
        if (actorRole == MembershipRole.OWNER) {
            return;
        }
        if (actorRole == MembershipRole.ADMIN && assignedRole == MembershipRole.STAFF) {
            return;
        }
        throw new AccessDeniedException("Role assignment is not permitted");
    }

    private void requireCanManageRole(MembershipRole actorRole, MembershipRole targetRole) {
        if (actorRole == MembershipRole.OWNER) {
            return;
        }
        if (actorRole == MembershipRole.ADMIN && targetRole == MembershipRole.STAFF) {
            return;
        }
        throw new AccessDeniedException("Member management is not permitted");
    }

    private void requireAnotherOwner(Long businessId) {
        if (membershipRepository.countByBusinessIdAndRoleAndActiveTrue(
                businessId, MembershipRole.OWNER
        ) <= 1) {
            throw new BadRequestException("Business must retain at least one active owner");
        }
    }

    private Business lockBusiness(Long businessId) {
        return businessRepository.findByIdForUpdate(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
    }

    private BusinessMembership requireMembership(Long businessId, Long membershipId) {
        return membershipRepository.findByIdAndBusinessId(membershipId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business member not found"));
    }

    private User requireActiveUser(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Active user not found"));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private MemberResponse toMemberResponse(BusinessMembership membership) {
        User user = membership.getUser();
        return new MemberResponse(
                membership.getId(), user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), membership.getRole(), membership.isActive(),
                membership.getCreatedAt(), membership.getUpdatedAt()
        );
    }

    private InvitationResponse toInvitationResponse(BusinessInvitation invitation) {
        return new InvitationResponse(
                invitation.getId(), invitation.getBusiness().getId(), invitation.getEmail(),
                invitation.getRole(), invitation.getStatus(), invitation.getExpiresAt(),
                invitation.getInvitedBy().getEmail(), invitation.getCreatedAt()
        );
    }
}
