package io.sala.krob_krong.account.service.impl;

import io.sala.krob_krong.account.dto.VerificationResponse;
import io.sala.krob_krong.account.service.*;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.school.service.SchoolAccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {
    private final EmailVerificationStore store;
    private final JavaMailSender mail;
    private final String from;
    private final String verificationUrl;

    public EmailVerificationServiceImpl(
            EmailVerificationStore store,
            JavaMailSender mail,
            @Value("${krob-krong.account.verification.from}") String from,
            @Value("${krob-krong.account.verification.url}") String verificationUrl) {
        this.store = store;
        this.mail = mail;
        this.from = from;
        this.verificationUrl = verificationUrl;
    }

    @Override
    public VerificationResponse request() {
        var pending = store.prepare(SchoolAccess.actor());
        var message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(pending.getEmail());
        message.setSubject("Verify your Krob Krong email");
        String link = UriComponentsBuilder.fromUriString(verificationUrl)
                .queryParam("token", pending.getRawToken())
                .build()
                .toUriString();
        message.setText("Verify your email to register a school:\n\n" + link
                + "\n\nThis link expires in 24 hours. If you did not request it, you can ignore this email.");
        try {
            mail.send(message);
        } catch (MailException ex) {
            try {
                store.cancel(pending.getId());
            } catch (RuntimeException cleanup) {
                ex.addSuppressed(cleanup);
            }
            throw new BusinessException(IAMErrorCode.VERIFICATION_DELIVERY_FAILED);
        }
        return new VerificationResponse(false, pending.getExpiresAt());
    }

    @Override
    public VerificationResponse confirm(String token) {
        store.confirm(token);
        return new VerificationResponse(true, null);
    }
}
