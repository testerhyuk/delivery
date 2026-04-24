package com.hyuk.pay.service;

import com.hyuk.pay.dto.PayConfirmedRequestDto;
import com.hyuk.pay.dto.ResponseOrder;
import com.hyuk.pay.entity.PayEntity;
import com.hyuk.pay.entity.enums.PayStatus;
import com.hyuk.pay.repository.PayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
public class PayConfirmWriter {
    private final PayRepository payRepository;
    private final PayOutboxService payOutboxService;

    @Transactional
    public void saveConfirmResult(PayEntity entity, ResponseOrder response, String orderId) {
        String cardNumber = response.getCard() != null ? response.getCard().getNumber() : null;

        entity.completedPayment(response.getPaymentKey(), response.getMethod(),
                cardNumber, response.getVat(), parseApprovedAt(response.getApprovedAt()));

        payRepository.save(entity);

        PayConfirmedRequestDto dto = new PayConfirmedRequestDto();
        dto.setOrderId(orderId);
        dto.setPayStatus(PayStatus.DONE.name());

        payOutboxService.saveToOrderUpdatePaid(dto);
    }

    private LocalDateTime parseApprovedAt(String approvedAt) {
        if (approvedAt == null || approvedAt.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(approvedAt).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.parse(approvedAt);
        }
    }
}
