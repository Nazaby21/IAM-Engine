package io.sala.krob_krong.enrollment.service.impl;

import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.enrollment.service.TicketService;
import java.time.Duration;

import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.utils.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {
    private final StringRedisTemplate redisTemplate;
    private final IdGenerator idGenerator;

    @Override
    public String generate(String id) {
        String ticketId = idGenerator.getTicketId();
        Duration ttl = Duration.ofMinutes(1);

        redisTemplate.opsForValue().set("enrollment:ticket:" + ticketId, id, ttl);
        redisTemplate.opsForValue().set("enrollment:active-ticket:" + id, ticketId, ttl);
        return ticketId;
    }

    @Override
    public boolean exists(String id) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("enrollment:active-ticket:" + id));
    }

    @Override
    public String getEnrollmentId(String ticketNumber) {
        String enrollmentId= redisTemplate.opsForValue().get("enrollment:ticket:" + ticketNumber);

        if (ObjectUtils.isEmpty(enrollmentId)){
            throw new BusinessException(IAMErrorCode.TICKET_NOT_FOUND);
        }
        return enrollmentId;
    }

    @Override
    public String getTicketNumber(String enrollmentId) {
        return redisTemplate.opsForValue().get(
                "enrollment:active-ticket:" + enrollmentId
        );
    }
}
