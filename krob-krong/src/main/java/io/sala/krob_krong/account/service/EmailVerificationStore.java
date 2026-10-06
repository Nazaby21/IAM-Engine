package io.sala.krob_krong.account.service;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

public interface EmailVerificationStore {
    Pending prepare(UUID userId);

    void cancel(UUID tokenId);

    void confirm(String rawToken);

    @Getter
    @AllArgsConstructor
    class Pending {
        private UUID id;
        private String email;
        private String rawToken;
        private Instant expiresAt;
    }
}
