package io.sala.krob_krong.enrollment.service.impl;

import io.sala.krob_krong.enrollment.service.TicketService;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {
    private final StringRedisTemplate redisTemplate;

    @Override
    public String generate(String id) {
        String ticketId = "TK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Duration ttl = Duration.ofMinutes(1);

        redisTemplate.opsForValue().set("enrollment:ticket:" + ticketId, "PENDING", ttl);
        redisTemplate.opsForValue().set("enrollment:active-ticket:" + id, ticketId, ttl);
        return ticketId;
    }

    @Override
    public boolean exists(String id) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("enrollment:active-ticket:" + id));
    }
}
