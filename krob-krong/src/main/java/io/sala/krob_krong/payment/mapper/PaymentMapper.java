package io.sala.krob_krong.payment.mapper;

import io.sala.krob_krong.payment.dto.request.PaymentMakeRequest;
import io.sala.krob_krong.payment.dto.response.PaymentResponse;
import io.sala.krob_krong.payment.entity.PaymentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PaymentMapper {
    PaymentEntity toEntity(PaymentMakeRequest request);

    PaymentResponse toPayment(PaymentEntity entity);
}
