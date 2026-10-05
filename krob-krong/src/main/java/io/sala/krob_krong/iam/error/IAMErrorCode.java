package io.sala.krob_krong.iam.error;

import io.sala.krob_krong.common.errors.ErrorCode;

public enum IAMErrorCode implements ErrorCode {
    EMAIL_ALREADY_REGISTERED(409, "That email address is already registered"),
    INVALID_CREDENTIALS(401, "Incorrect email or password"),
    ACCOUNT_SUSPENDED(403, "This account is suspended"),

    SCHOOL_NOT_FOUND(404, "School not found"),
    SCHOOL_STATUS_INVALID(400, "School cannot transition to the requested status"),
    SCHOOL_PROFILE_LOCKED(400, "The school profile is locked while under review"),
    SCHOOL_PROFILE_INCOMPLETE(400, "The school profile is incomplete for submission"),

    BRANCH_NOT_FOUND(404, "Branch not found"),
    BRANCH_NOT_READY(400, "The branch needs an address, location and workspace before it opens"),

    WORKSPACE_SLUG_TAKEN(409, "That workspace name is already taken"),
    WORKSPACE_SLUG_RESERVED(409, "That workspace name is reserved"),
    WORKSPACE_NOT_FOUND(404, "Workspace not found"),

    ROLE_NOT_FOUND(404, "Role not found"),
    ROLE_GRANT_NOT_ALLOWED(403, "You may not grant or revoke this role"),
    ROLE_ALREADY_HELD(409, "User already holds this role in this scope"),

    NOT_A_MEMBER(403, "You are not a member of this school"),
    INVITATION_NOT_FOUND(404, "Invitation not found"),
    INVITATION_ALREADY_ACCEPTED(400, "This invitation has already been accepted"),
    LAST_OWNER(400, "A school must keep at least one owner"),

    MEDIA_NOT_FOUND(404, "Media asset not found"),
    MEDIA_NOT_READY(400, "This file is not ready yet"),
    MEDIA_IN_USE(400, "This file is in use as a logo, cover or avatar"),

    REVIEW_CONFLICT_OF_INTEREST(403, "Reviewers cannot decide on a school they belong to"),

    SUPPORT_SESSION_EXPIRED(403, "The support session has ended or expired"),
    SUPPORT_MFA_REQUIRED(403, "Platform roles require MFA"),

    INVALID_REFRESH_TOKEN(401, "The session has expired or is invalid"),
    REFRESH_TOKEN_REUSE(401, "This session was revoked for security reasons; please sign in again");

    private final int httpStatus;
    private final String defaultMessage;

    IAMErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public int httpStatus() {
        return httpStatus;
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }
}
