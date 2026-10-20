package io.sala.krob_krong.account.service.impl;

import io.sala.krob_krong.account.dto.MfaStatusResponse;
import io.sala.krob_krong.account.dto.TotpEnrollmentResponse;
import io.sala.krob_krong.account.entity.MfaTotpEntity;
import io.sala.krob_krong.account.entity.UserEntity;
import io.sala.krob_krong.account.mfa.MfaProperties;
import io.sala.krob_krong.account.mfa.Totp;
import io.sala.krob_krong.account.mfa.TotpSecretCipher;
import io.sala.krob_krong.account.repository.MfaTotpRepository;
import io.sala.krob_krong.account.repository.UserRepository;
import io.sala.krob_krong.account.service.MfaService;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import java.time.Instant;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MfaServiceImpl implements MfaService {

    private final UserRepository users;
    private final MfaTotpRepository totps;
    private final TotpSecretCipher cipher;
    private final MfaProperties props;

    @Override
    @Transactional
    public TotpEnrollmentResponse startTotpEnrollment(UUID userId) {
        UserEntity user = lockPerson(userId);
        if (user.getMfaEnabledAt() != null) {
            throw new BusinessException(IAMErrorCode.MFA_ALREADY_ENABLED);
        }
        byte[] secret = Totp.newSecret();
        MfaTotpEntity totp = totps.findById(userId).orElseGet(MfaTotpEntity::new);
        totp.setUserId(userId);
        totp.setSecretCiphertext(cipher.encrypt(userId, secret));
        totp.setCreatedAt(Instant.now());
        totp.setActivatedAt(null);
        totp.setLastUsedStep(null);
        totp.setFailedAttempts(0);
        totp.setLockedUntil(null);
        totps.save(totp);
        String account = Objects.requireNonNullElse(user.getEmail(), userId.toString());
        return new TotpEnrollmentResponse(Totp.base32(secret), Totp.uri(props.getIssuer(), account, secret));
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public MfaStatusResponse activateTotp(UUID userId, String code) {
        UserEntity user = lockPerson(userId);
        if (user.getMfaEnabledAt() != null) {
            throw new BusinessException(IAMErrorCode.MFA_ALREADY_ENABLED);
        }
        MfaTotpEntity totp =
                totps.findById(userId).orElseThrow(() -> new BusinessException(IAMErrorCode.MFA_ENROLLMENT_NOT_FOUND));
        consume(totp, code);
        Instant now = Instant.now();
        totp.setActivatedAt(now);
        user.setMfaEnabledAt(now);
        return new MfaStatusResponse(now);
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public void verifyLoginCode(UUID userId, String code) {
        MfaTotpEntity totp = totps.findByIdForUpdate(userId)
                .filter(MfaTotpEntity::isActive)
                .orElseThrow(() -> new BusinessException(IAMErrorCode.INVALID_MFA_CODE));
        consume(totp, code);
    }

    private UserEntity lockPerson(UUID userId) {
        return users.findByIdForUpdate(userId)
                .filter(UserEntity::isPerson)
                .orElseThrow(() -> new BusinessException(IAMErrorCode.USER_NOT_FOUND));
    }

    private void consume(MfaTotpEntity totp, String code) {
        Instant now = Instant.now();
        if (totp.getLockedUntil() != null && totp.getLockedUntil().isAfter(now)) {
            throw new BusinessException(IAMErrorCode.MFA_LOCKED);
        }
        byte[] secret = cipher.decrypt(totp.getUserId(), totp.getSecretCiphertext());
        OptionalLong step = Totp.match(secret, code, now, totp.getLastUsedStep());
        if (step.isEmpty()) {
            int failures = totp.getFailedAttempts() + 1;
            if (failures >= props.getMaxFailedAttempts()) {
                totp.setFailedAttempts(0);
                totp.setLockedUntil(now.plus(props.getLockout()));
            } else {
                totp.setFailedAttempts(failures);
            }
            throw new BusinessException(IAMErrorCode.INVALID_MFA_CODE);
        }
        totp.setLastUsedStep(step.getAsLong());
        totp.setFailedAttempts(0);
        totp.setLockedUntil(null);
    }
}
