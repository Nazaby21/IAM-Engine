package io.sala.krob_krong.iam.error;

import io.sala.krob_krong.common.errors.ErrorCode;

public enum IAMErrorCode implements ErrorCode {
    EMAIL_ALREADY_REGISTERED(409, "That email address is already registered"),
    INVALID_CREDENTIALS(401, "Incorrect email or password"),
    ACCOUNT_SUSPENDED(403, "This account is suspended"),
    TENANT_SLUG_TAKEN(409, "That workspace URL is already taken"),
    TENANT_NOT_FOUND(404, "Workspace not found"),
    NOT_A_MEMBER(403, "You are not a member of this workspace"),
    MEMBERSHIP_NOT_FOUND(404, "Membership not found"),
    ROLE_NOT_FOUND(404, "Role not found"),
    ROLE_KEY_TAKEN(409, "A role with that key already exists in this workspace"),
    SYSTEM_ROLE_IMMUTABLE(403, "System roles cannot be modified"),
    PRIVILEGE_CEILING_EXCEEDED(403, "You cannot manage or grant a role ranked at or above your own"),
    ROLE_IN_USE(409, "This role is still assigned to members and cannot be deleted"),
    UNKNOWN_PERMISSION(400, "One or more permissions are not in the catalog"),
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
