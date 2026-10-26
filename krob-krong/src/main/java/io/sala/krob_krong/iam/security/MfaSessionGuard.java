package io.sala.krob_krong.iam.security;

import io.sala.krob_krong.account.entity.UserEntity;
import io.sala.krob_krong.account.repository.UserRepository;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MfaSessionGuard {

    private final UserRepository users;

    public void require() {
        AuthenticatedUser principal =
                Principals.current().orElseThrow(() -> new BusinessException(IAMErrorCode.MFA_REQUIRED));
        boolean enrolled =
                users.findById(principal.id()).map(UserEntity::getMfaEnabledAt).isPresent();
        if (!principal.mfaVerified() || !enrolled) {
            throw new BusinessException(IAMErrorCode.MFA_REQUIRED);
        }
    }
}
