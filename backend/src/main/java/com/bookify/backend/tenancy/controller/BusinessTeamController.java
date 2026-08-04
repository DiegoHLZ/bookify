package com.bookify.backend.tenancy.controller;

import com.bookify.backend.common.SecurityUtils;
import com.bookify.backend.tenancy.dto.*;
import com.bookify.backend.tenancy.service.BusinessTeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BusinessTeamController {
    private final BusinessTeamService teamService;

    public BusinessTeamController(BusinessTeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/api/v1/businesses/{businessId}/members")
    public List<MemberResponse> members(@PathVariable Long businessId) {
        return teamService.listMembers(businessId, SecurityUtils.getCurrentUserEmail());
    }

    @GetMapping("/api/v1/businesses/{businessId}/permissions/me")
    public MembershipPermissionsResponse permissions(@PathVariable Long businessId) {
        return teamService.permissions(businessId, SecurityUtils.getCurrentUserEmail());
    }

    @PostMapping("/api/v1/businesses/{businessId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedInvitationResponse invite(
            @PathVariable Long businessId,
            @Valid @RequestBody CreateInvitationRequest request
    ) {
        return teamService.invite(
                businessId, request, SecurityUtils.getCurrentUserEmail()
        );
    }

    @GetMapping("/api/v1/businesses/{businessId}/invitations")
    public List<InvitationResponse> invitations(@PathVariable Long businessId) {
        return teamService.listInvitations(
                businessId, SecurityUtils.getCurrentUserEmail()
        );
    }

    @DeleteMapping("/api/v1/businesses/{businessId}/invitations/{invitationId}")
    public InvitationResponse revoke(
            @PathVariable Long businessId,
            @PathVariable Long invitationId
    ) {
        return teamService.revokeInvitation(
                businessId, invitationId, SecurityUtils.getCurrentUserEmail()
        );
    }

    @PostMapping("/api/v1/invitations/{token}/accept")
    public MemberResponse accept(@PathVariable String token) {
        return teamService.acceptInvitation(token, SecurityUtils.getCurrentUserEmail());
    }

    @PatchMapping("/api/v1/businesses/{businessId}/members/{membershipId}/role")
    public MemberResponse changeRole(
            @PathVariable Long businessId,
            @PathVariable Long membershipId,
            @Valid @RequestBody ChangeMemberRoleRequest request
    ) {
        return teamService.changeRole(
                businessId, membershipId, request.role(),
                SecurityUtils.getCurrentUserEmail()
        );
    }

    @PatchMapping("/api/v1/businesses/{businessId}/members/{membershipId}/status")
    public MemberResponse changeStatus(
            @PathVariable Long businessId,
            @PathVariable Long membershipId,
            @Valid @RequestBody ChangeMemberStatusRequest request
    ) {
        return teamService.changeStatus(
                businessId, membershipId, request.active(),
                SecurityUtils.getCurrentUserEmail()
        );
    }
}
