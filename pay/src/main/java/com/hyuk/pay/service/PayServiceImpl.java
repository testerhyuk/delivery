package com.hyuk.pay.service;

import com.hyuk.common.Snowflake;
import com.hyuk.pay.client.OrderServiceClient;
import com.hyuk.pay.client.TossPayClient;
import com.hyuk.pay.dto.*;
import com.hyuk.pay.entity.PayEntity;
import com.hyuk.pay.entity.enums.PayStatus;
import com.hyuk.pay.repository.PayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final OrderServiceClient orderServiceClient;
    private final PayLogService payLogService;

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
    @Retryable(
            retryFor = { feign.RetryableException.class, java.net.ConnectException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ResponseOrder confirmPayment(RequestOrder requestOrder) {
        PayEntity entity = payRepository.findByOrderId(requestOrder.getOrderId())
                .orElseThrow(() -> new RuntimeException("결제 정보가 존재하지 않습니다. 주문번호: " + requestOrder.getOrderId()));

        if (PayStatus.CANCELED.equals(entity.getStatus())) {
            throw new RuntimeException("이미 취소된 주문번호입니다. 새로운 주문이 필요합니다.");
        }

        if (PayStatus.DONE.equals(entity.getStatus())) {
            return modelMapper.map(entity, ResponseOrder.class);
        }

        if (!entity.getAmount().equals(requestOrder.getAmount())) {
            throw new RuntimeException(String.format("결제 금액이 다릅니다. 실제 금액 : %d, 요청 금액 : %d",
                    entity.getAmount(), requestOrder.getAmount()));
        }

        ResponseOrder response = tossPayClient.confirm(requestOrder);

        entity.completedPayment(response.getPaymentKey(), response.getMethod(),
                String.valueOf(response.getCard().getNumber()), response.getVat(), response.getApprovedAt());

        payRepository.save(entity);

        PayConfirmedRequestDto payConfirmedRequestDto = new PayConfirmedRequestDto();
        payConfirmedRequestDto.setOrderId(response.getOrderId());
        payConfirmedRequestDto.setPayStatus(String.valueOf(response.getPayStatus()));

        orderServiceClient.updatePaid(payConfirmedRequestDto);

        return response;
    }

    @Recover
    public ResponseOrder recover(Exception e, RequestOrder requestOrder) {
        log.error("결제 최종 실패 (3회 시도 종료) : {}", e.getMessage());

        payRepository.findByOrderId(requestOrder.getOrderId()).ifPresent(entity -> {
            payLogService.recordFail(
                    snowflake.nextId(),
                    entity.getId(),
                    "최종 실패 사유: " + e.getMessage(),
                    LocalDateTime.now()
            );
        });

        throw new RuntimeException("결제 승인 최종 실패: " + e.getMessage());
    }

    @Override
    public void cancelPayment(TossCancelResponse response) {
        PayEntity entity = payRepository.findByOrderId(response.getOrderId()).orElseThrow(() -> new RuntimeException("결제 정보 없음"));

        if (response.getCancels() != null && !response.getCancels().isEmpty()) {
            TossCancelResponse.Cancel lastCancel = response.getCancels().getLast();

            entity.cancelPayment(lastCancel.getCancelReason(), LocalDateTime.parse(lastCancel.getCanceledAt()));
        }

        payRepository.save(entity);
    }
}
