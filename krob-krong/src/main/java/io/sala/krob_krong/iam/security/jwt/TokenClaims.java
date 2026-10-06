package io.sala.krob_krong.iam.security.jwt;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TokenClaims {

    /** RFC 9068 media type; stops other JWTs signed with the same key being replayed as access tokens. */
    public static final String ACCESS_TOKEN_TYPE = "at+jwt";

    public static final String SESSION_ID = "sid";
    public static final String KIND = "kind";
    public static final String MFA = "mfa";
    public static final String NAME = "name";
    public static final String EMAIL = "email";
}
