package io.sala.krob_krong.enrollment.service;

public interface TicketService {
    String generate(String id);

    boolean exists(String id);
}
