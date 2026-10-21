package io.sala.krob_krong.enrollment.service.impl;

import io.sala.krob_krong.enrollment.constant.EnrollmentStatus;
import io.sala.krob_krong.enrollment.dto.request.EnrollmentRequest;
import io.sala.krob_krong.enrollment.dto.response.EnrollmentResponse;
import io.sala.krob_krong.enrollment.dto.response.TicketResponse;
import io.sala.krob_krong.enrollment.entity.EnrollmentEntity;
import io.sala.krob_krong.enrollment.mapper.EnrollmentMapper;
import io.sala.krob_krong.enrollment.repository.EnrollmentRepository;
import io.sala.krob_krong.enrollment.service.EnrollmentService;
import io.sala.krob_krong.enrollment.service.TicketService;
import io.sala.krob_krong.enrollment.service.validator.EnrollmentValidator;
import io.sala.krob_krong.enrollment.utils.IdGenerator;
import io.sala.krob_krong.school.service.SchoolAccess;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {
    private final EnrollmentMapper enrollmentMapper;
    private final EnrollmentRepository enrollmentRepository;
    private final TicketService ticketService;
    private final EnrollmentValidator enrollmentValidator;
    private final IdGenerator idGenerator;

    @Override
    public EnrollmentResponse create(EnrollmentRequest request) {
        EnrollmentEntity entity = enrollmentMapper.toEntity(request);
        UUID actor = SchoolAccess.actor();
        enrollmentValidator.validate(request);
        entity.setId(idGenerator.enroll());
        entity.setStatus(EnrollmentStatus.PENDING.name());
        entity.setCreatedBy(actor);
        EnrollmentEntity save = enrollmentRepository.save(entity);
        String ticket = ticketService.generate(save.getId());
        EnrollmentResponse response = enrollmentMapper.toResponse(save);
        response.setStatus(EnrollmentStatus.PENDING);
        response.setTicketNumber(ticket);
        response.setExpiresIn(60);
        return response;
    }

    @Override
    public EnrollmentResponse update(String id, EnrollmentRequest request) {
        EnrollmentEntity entity = enrollmentValidator.findById(id);
        enrollmentMapper.updateEntity(request, entity);
        entity.setUpdatedBy(SchoolAccess.actor());
        EnrollmentEntity saved = enrollmentRepository.save(entity);
        return enrollmentMapper.toResponse(saved);
    }

    @Override
    public TicketResponse exchange(String id) {
        EnrollmentEntity entity = enrollmentValidator.findById(id);
        enrollmentValidator.isPending(entity);
        enrollmentValidator.isTicketExist(id);
        String ticketId = ticketService.generate(id);
        TicketResponse response = enrollmentMapper.toTicket(entity);
        response.setTicketNumber(ticketId);
        return response;
    }
}
