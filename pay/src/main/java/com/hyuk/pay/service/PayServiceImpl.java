package com.hyuk.pay.service;

import com.hyuk.common.Snowflake;
import com.hyuk.pay.client.TossPayClient;
import com.hyuk.pay.dto.RequestOrder;
import com.hyuk.pay.dto.ResponseOrder;
import com.hyuk.pay.dto.ResponsePayReady;
import com.hyuk.pay.entity.PayEntity;
import com.hyuk.pay.entity.enums.PayStatus;
import com.hyuk.pay.repository.PayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PayServiceImpl implements PayService {
    private final PayRepository payRepository;
    private final TossPayClient tossPayClient;
    private final Snowflake snowflake;
    private final ModelMapper modelMapper;

    @Override
    public ResponsePayReady readyPayment(RequestOrder requestOrder) {
        Optional<PayEntity> existingPay = payRepository.findByOrderId(requestOrder.getOrderId());

        if (existingPay.isPresent()) {
            PayEntity payHistory = existingPay.get();

            if (payHistory.getStatus() != PayStatus.READY) {
                throw new RuntimeException("이미 처리가 진행 중이거나 완료된 주문입니다.");
            }

            if (!payHistory.getAmount().equals(requestOrder.getAmount())) {
                payHistory.updateAmount(requestOrder.getAmount());
            }

            return modelMapper.map(payHistory, ResponsePayReady.class);
        }

        PayEntity entity = PayEntity.processPay(
                snowflake.nextId(),
                requestOrder.getOrderId(),
                requestOrder.getAmount()
        );

        payRepository.save(entity);

        return modelMapper.map(entity, ResponsePayReady.class);
    }

    @Override
    public ResponseOrder confirmPayment(RequestOrder requestOrder) {
        PayEntity entity = payRepository.findByOrderId(requestOrder.getOrderId())
                .orElseThrow(() -> new RuntimeException("결제 정보가 존재하지 않습니다. 주문번호: " + requestOrder.getOrderId()));

        if (!entity.getAmount().equals(requestOrder.getAmount())) {
            throw new RuntimeException(String.format("결제 금액이 다릅니다. 실제 금액 : %d, 요청 금액 : %d",
                    entity.getAmount(), requestOrder.getAmount()));
        }

        try {
            ResponseOrder response = tossPayClient.confirm(requestOrder);

            entity.completedPayment(response.getPaymentKey(), response.getMethod(),
                    String.valueOf(response.getCard().getNumber()),response.getVat(), response.getApprovedAt());

            payRepository.save(entity);

            return response;
        } catch (Exception e) {
            log.error("결제 실패 : {}", e.getMessage());
            entity.failPayment(e.getMessage());
            throw e;
        }
    }
}
