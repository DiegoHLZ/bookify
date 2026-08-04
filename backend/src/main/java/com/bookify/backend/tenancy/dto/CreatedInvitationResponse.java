package com.bookify.backend.tenancy.dto;

public record CreatedInvitationResponse(
        InvitationResponse invitation,
        String invitationToken
) {
}
