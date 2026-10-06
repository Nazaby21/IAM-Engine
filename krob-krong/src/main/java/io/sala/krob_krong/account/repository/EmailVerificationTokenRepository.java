package io.sala.krob_krong.account.repository;

import io.sala.krob_krong.account.entity.EmailVerificationTokenEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface EmailVerificationTokenRepository
        extends JpaRepository<EmailVerificationTokenEntity, UUID>,
                JpaSpecificationExecutor<EmailVerificationTokenEntity> {}
