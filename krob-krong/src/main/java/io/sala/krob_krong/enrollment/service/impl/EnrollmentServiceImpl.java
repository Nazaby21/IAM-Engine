package io.sala.krob_krong.enrollment.service.impl;

import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.enrollment.constant.EnrollmentStatus;
import io.sala.krob_krong.enrollment.dto.request.EnrollmentCreateRequest;
import io.sala.krob_krong.enrollment.dto.response.EnrollmentResponse;
import io.sala.krob_krong.enrollment.entity.EnrollmentEntity;
import io.sala.krob_krong.enrollment.mapper.EnrollmentMapper;
import io.sala.krob_krong.enrollment.repository.EnrollmentRepository;
import io.sala.krob_krong.enrollment.service.EnrollmentService;
import io.sala.krob_krong.enrollment.service.TicketService;
import io.sala.krob_krong.enrollment.service.validator.EnrollmentValidator;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.school.service.SchoolAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {
    private final EnrollmentMapper enrollmentMapper;
    private final EnrollmentRepository enrollmentRepository;
    private final TicketService ticketService;
    private final EnrollmentValidator enrollmentValidator;

    @Override
    public EnrollmentResponse create(EnrollmentCreateRequest request) {
        EnrollmentEntity entity = enrollmentMapper.toEntity(request);
        UUID actor = SchoolAccess.actor();
        enrollmentValidator.validate(request);
        entity.setStatus(EnrollmentStatus.PENDING.name());
        entity.setCreatedBy(actor);
        EnrollmentEntity save = enrollmentRepository.save(entity);
        String ticket = ticketService.generate(save.getId());
        EnrollmentResponse response=enrollmentMapper.toResponse(save);
        response.setStatus(EnrollmentStatus.PENDING);
        response.setTicketId(ticket);
        response.setExpiresIn(60);
        return response ;
    }

    @Override
    public EnrollmentResponse exchange(UUID id) {
        EnrollmentEntity entity = enrollmentValidator.findById(id);

        if (!EnrollmentStatus.PENDING.name().equals(entity.getStatus())) {
            throw new BusinessException(IAMErrorCode.CANNOT_EXCHANGE);
        }

        if (ticketService.exists(id)) {
            throw new BusinessException(IAMErrorCode.TICKET_EXISTING);
        }

        String ticketId= ticketService.generate(id);
        EnrollmentResponse response = enrollmentMapper.toResponse(entity);
        response.setTicketId(ticketId);
        return response;
    }
}
