package io.sala.krob_krong.payment.mapper;

import io.sala.krob_krong.payment.dto.request.PaymentMakeRequest;
import io.sala.krob_krong.payment.dto.response.PaymentResponse;
import io.sala.krob_krong.payment.entity.PaymentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentEntity toEntity(PaymentMakeRequest request);

    PaymentResponse toPayment(PaymentEntity entity);
}
