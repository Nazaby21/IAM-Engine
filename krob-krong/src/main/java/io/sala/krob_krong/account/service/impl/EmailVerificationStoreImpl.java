package io.sala.krob_krong.account.service.impl;

import io.sala.krob_krong.account.entity.*;
import io.sala.krob_krong.account.repository.*;
import io.sala.krob_krong.account.service.EmailVerificationStore;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationStoreImpl implements EmailVerificationStore {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository users;
    private final EmailVerificationTokenRepository tokens;

    @Override
    @Transactional
    public Pending prepare(UUID userId) {
        var user =
                users.findByIdForUpdate(userId).orElseThrow(() -> new BusinessException(IAMErrorCode.USER_NOT_FOUND));
        if (!user.isPerson() || !user.isActive() || user.getEmail() == null)
            throw new BusinessException(IAMErrorCode.INVALID_CREDENTIALS);
        if (user.isVerified()) throw new BusinessException(IAMErrorCode.EMAIL_ALREADY_VERIFIED);
        Instant now = Instant.now();
        Specification<EmailVerificationTokenEntity> owned =
                (root, q, cb) -> cb.equal(root.get(EmailVerificationTokenEntity.Fields.userId), userId);
        var recent = tokens.findAll(
                owned,
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, EmailVerificationTokenEntity.Fields.createdAt)));
        if (recent.hasContent()
                && recent.getContent().getFirst().getCreatedAt().plusSeconds(60).isAfter(now))
            throw new BusinessException(IAMErrorCode.VERIFICATION_RATE_LIMITED);
        tokens.findAll(owned.and((root, q, cb) -> cb.isNull(root.get(EmailVerificationTokenEntity.Fields.consumedAt))))
                .forEach(t -> t.setConsumedAt(now));
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = HexFormat.of().formatHex(bytes);
        var token = new EmailVerificationTokenEntity();
        token.setUserId(userId);
        token.setEmail(user.getEmail());
        token.setTokenHash(hash(raw));
        token.setCreatedAt(now);
        token.setExpiresAt(now.plus(Duration.ofHours(24)));
        tokens.save(token);
        return new Pending(token.getId(), token.getEmail(), raw, token.getExpiresAt());
    }

    @Override
    @Transactional
    public void cancel(UUID tokenId) {
        tokens.deleteById(tokenId);
    }

    @Override
    @Transactional
    public void confirm(String raw) {
        if (raw == null || !raw.matches("[0-9a-f]{64}")) throw invalid();
        Specification<EmailVerificationTokenEntity> byHash =
                (root, q, cb) -> cb.equal(root.get(EmailVerificationTokenEntity.Fields.tokenHash), hash(raw));
        var token = tokens.findOne(byHash).orElseThrow(EmailVerificationStoreImpl::invalid);
        var user = users.findByIdForUpdate(token.getUserId()).orElseThrow(EmailVerificationStoreImpl::invalid);
        // Refresh after obtaining the user lock so concurrent confirmations cannot reuse a cached token.
        refreshToken(token);
        if (token.getConsumedAt() != null
                || !token.getExpiresAt().isAfter(Instant.now())
                || !user.isPerson()
                || !user.isActive()
                || !token.getEmail().equals(user.getEmail())) throw invalid();
        user.setVerifiedAt(Instant.now());
        token.setConsumedAt(Instant.now());
        users.save(user);
        tokens.save(token);
    }

    private final jakarta.persistence.EntityManager em;

    private void refreshToken(EmailVerificationTokenEntity token) {
        em.refresh(token);
    }

    private static BusinessException invalid() {
        return new BusinessException(IAMErrorCode.INVALID_VERIFICATION_TOKEN);
    }

    private static String hash(String raw) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
