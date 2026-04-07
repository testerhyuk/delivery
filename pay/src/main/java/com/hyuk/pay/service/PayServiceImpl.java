package com.hyuk.pay.service;

import com.hyuk.common.Snowflake;
import com.hyuk.pay.client.TossPayClient;
import com.hyuk.pay.dto.RequestOrder;
import com.hyuk.pay.dto.ResponseOrder;
import com.hyuk.pay.entity.PayEntity;
import com.hyuk.pay.entity.enums.PayStatus;
import com.hyuk.pay.repository.PayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PayServiceImpl implements PayService {
    private final PayRepository payRepository;
    private final TossPayClient tossPayClient;
    private final Snowflake snowflake;

    @Override
    public void readyPayment(RequestOrder requestOrder) {
        PayEntity payHistory = payRepository.findByOrderId(requestOrder.getOrderId());

        if (payHistory != null) {
            if (payHistory.getStatus() == PayStatus.READY) {
                if (!payHistory.getAmount().equals(requestOrder.getAmount())) {
                    payHistory.updateAmount(requestOrder.getAmount());
                }
            }

            return;
        }

        PayEntity entity = PayEntity.processPay(snowflake.nextId(), requestOrder.getOrderId(), requestOrder.getAmount());
        payRepository.save(entity);
    }

    @Override
    public ResponseOrder confirmPayment(RequestOrder requestOrder) {
        PayEntity entity = payRepository.findByOrderId(requestOrder.getOrderId());

        if (entity == null) {
            throw new RuntimeException("결제 정보 없음");
        }

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
