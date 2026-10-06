package io.sala.krob_krong.iam.security.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionClaims {
    private UUID userId;
    private UUID sessionId;
    private String kind;
    private String displayName;
    private String email;
    private boolean mfaVerified;
}
