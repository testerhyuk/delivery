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
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayServiceImpl implements PayService {
    private final PayRepository payRepository;
    private final TossPayClient tossPayClient;
    private final Snowflake snowflake;
    private final PayLogService payLogService;
    private final PayOutboxService payOutboxService;
    private final PayConfirmWriter payConfirmWriter;

    @Transactional
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

            return convertToReadyResponse(payHistory);
        }

        PayEntity entity = PayEntity.processPay(
                snowflake.nextId(),
                requestOrder.getOrderId(),
                requestOrder.getAmount()
        );

        payRepository.save(entity);

        return convertToReadyResponse(entity);
    }

    @Override
    @Retryable(
            retryFor = { feign.RetryableException.class, java.net.ConnectException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ResponseOrder confirmPayment(RequestOrder requestOrder) {
        PayEntity entity = validateAndGet(requestOrder);

        ResponseOrder response = tossPayClient.confirm(requestOrder);

        payConfirmWriter.saveConfirmResult(entity, response, requestOrder.getOrderId());

        return response;
    }

    @Transactional(readOnly = true)
    public PayEntity validateAndGet(RequestOrder requestOrder) {
        PayEntity entity = payRepository.findByOrderId(requestOrder.getOrderId())
                .orElseThrow(() -> new RuntimeException("결제 정보가 존재하지 않습니다. 주문번호: " + requestOrder.getOrderId()));

        if (PayStatus.CANCELED.equals(entity.getStatus())) {
            throw new RuntimeException("이미 취소된 주문번호입니다. 새로운 주문이 필요합니다.");
        }

        // 동일 요청 방어 코드
        if (PayStatus.DONE.equals(entity.getStatus())) {
            return entity;
        }

        if (!entity.getAmount().equals(requestOrder.getAmount())) {
            throw new RuntimeException(String.format("결제 금액이 다릅니다. 실제 금액 : %d, 요청 금액 : %d",
                    entity.getAmount(), requestOrder.getAmount()));
        }

        return entity;
    }

    @Recover
    public ResponseOrder recover(Exception e, RequestOrder requestOrder) {
        log.error("결제 최종 실패 (3회 시도 종료) : {}", e.getMessage(), e);

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

    @Transactional
    @Override
    public void cancelPayment(TossCancelResponse response, boolean publishEvent) {
        try {
            PayEntity entity = payRepository.findByOrderId(response.getOrderId()).orElseThrow(() -> new RuntimeException("결제 정보 없음"));

            if (response.getCancels() != null && !response.getCancels().isEmpty()) {
                TossCancelResponse.Cancel lastCancel = response.getCancels().getLast();

                entity.cancelPayment(lastCancel.getCancelReason(), LocalDateTime.parse(lastCancel.getCanceledAt()));
            }

            payRepository.save(entity);

            if (publishEvent) {
                payOutboxService.saveToOrderCancel(response.getOrderId(), "CANCEL");
            }
        } catch (Exception e) {
            log.error("결제 취소 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("결제 취소 실패: " + e.getMessage(), e);
        }
    }

    @Transactional
    @Override
    public void userCancelProcess(String orderId, String reason) {
        cancelByOrderId(orderId, reason, true);
    }

    @Transactional
    @Override
    public void cancelByOrderId(String orderId, String reason, boolean publishEvent) {
        PayEntity entity = payRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("결제 내역을 찾을 수 없습니다."));

        if (entity.getPaymentKey() == null || entity.getPaymentKey().isBlank()) {
            throw new RuntimeException("결제 승인 전 상태라 취소할 수 없습니다.");
        }

        Map<String, String> tossRequest = Map.of("cancelReason", reason);
        TossCancelResponse response = tossPayClient.cancel(entity.getPaymentKey(), tossRequest);
        this.cancelPayment(response, publishEvent);
    }

    private ResponsePayReady convertToReadyResponse(PayEntity entity) {
        return ResponsePayReady.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .paymentKey(entity.getPaymentKey())
                .build();
    }
}
