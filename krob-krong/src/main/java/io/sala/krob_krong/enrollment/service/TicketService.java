package io.sala.krob_krong.enrollment.service;

import java.util.UUID;

public interface TicketService {
    String generate(UUID id);
    boolean exists(UUID id);
}
