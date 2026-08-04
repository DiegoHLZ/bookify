CREATE UNIQUE INDEX uk_pending_invitation_business_email
    ON business_invitations (business_id, LOWER(email))
    WHERE status = 'PENDING';
