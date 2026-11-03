package io.sala.krob_krong.enrollment.service;

public interface TicketService {
    String generate(String id);

    boolean exists(String id);

    String getEnrollmentId(String ticketNumber);

    String getTicketNumber(String enrollmentId);
}
