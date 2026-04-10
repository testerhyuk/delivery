package com.hyuk.pay.service;

import com.hyuk.pay.client.TossPayClient;
import com.hyuk.pay.dto.RequestOrder;
import com.hyuk.pay.dto.ResponseOrder;
import com.hyuk.pay.entity.PayEntity;
import com.hyuk.pay.repository.PayRepository;
import org.modelmapper.ModelMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayServiceImplTest {
    @Mock
    private TossPayClient tossPayClient;

    @Mock
    private PayRepository payRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PayLogService payLogService;

    @Mock
    private PayOutboxService payOutboxService;

    @InjectMocks
    private PayServiceImpl payService;

    @Test
    @DisplayName("결제금액이 다르면 예외 발생")
    void notEqualPriceMustThrowException() {
        String orderId = "order_123";
        PayEntity existingPay = PayEntity.processPay(1L, orderId, 21500L);
        when(payRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingPay));

        RequestOrder manipulatedRequest = new RequestOrder();
        manipulatedRequest.setOrderId(orderId);
        manipulatedRequest.setAmount(10000L);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            payService.confirmPayment(manipulatedRequest);
        });

        assertTrue(exception.getMessage().contains("결제 금액이 다릅니다"));
        verify(tossPayClient, never()).confirm(any());
    }

    @Test
    @DisplayName("결제 성공시 엔티티 상태가 완료로 변경")
    void statusMustBeDoneWhenCompletePay() {
        String orderId = "order_456";
        Long amount = 21500L;
        PayEntity existingPay = PayEntity.processPay(2L, orderId, amount);
        when(payRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingPay));

        ResponseOrder mockResponse = new ResponseOrder();
        mockResponse.setPaymentKey("toss_pk_abc123");
        mockResponse.setMethod("카드");
        mockResponse.setVat(1000L);
        mockResponse.setApprovedAt(LocalDateTime.parse("2026-04-07T16:00:00"));

        ResponseOrder.ResponseToss mockCard = new ResponseOrder.ResponseToss();
        mockCard.setNumber("12345678****1234");
        mockResponse.setCard(mockCard);

        when(tossPayClient.confirm(any())).thenReturn(mockResponse);

        RequestOrder request = new RequestOrder();
        request.setOrderId(orderId);
        request.setAmount(amount);

        payService.confirmPayment(request);

        verify(payRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("동일한 orderId로 요청이 왔을 때 기존 데이터 활용해서 금액 업데이트")
    void whenEqualOrderIdThenUpdatePriceUsingExistingData() {
        String orderId = "order_123";
        Long oldAmount = 20000L;
        Long newAmount = 25000L;

        PayEntity existingPay = PayEntity.processPay(1L, orderId, oldAmount);

        when(payRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingPay));

        RequestOrder updateRequest = new RequestOrder();
        updateRequest.setOrderId(orderId);
        updateRequest.setAmount(newAmount);

        payService.readyPayment(updateRequest);

        assertEquals(newAmount, existingPay.getAmount(), "금액이 최신 요청 금액으로 업데이트되어야 합니다.");

        verify(payRepository, never()).save(any(PayEntity.class));

        verify(payRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    @DisplayName("DB에 존재하지 않는 주문번호로 승인 요청 시 예외가 발생해야 한다")
    void shouldThrowExceptionWhenOrderNotFound() {
        String unknownOrderId = "NONE_123";
        when(payRepository.findByOrderId(unknownOrderId)).thenReturn(Optional.empty());

        RequestOrder request = new RequestOrder();
        request.setOrderId(unknownOrderId);
        request.setAmount(10000L);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            payService.confirmPayment(request);
        });

        assertTrue(exception.getMessage().contains("결제 정보가 존재하지 않습니다"));
        verify(tossPayClient, never()).confirm(any());
    }

    @Test
    @DisplayName("토스 승인 중 에러 발생 시 실패 사유를 DB에 기록해야 한다")
    void shouldRecordFailureReasonWhenTossReturnsError() {
        String orderId = "order_fail_test";
        PayEntity payEntity = PayEntity.processPay(1L, orderId, 21500L);
        when(payRepository.findByOrderId(orderId)).thenReturn(Optional.of(payEntity));

        String errorMessage = "잔액 부족";
        when(tossPayClient.confirm(any())).thenThrow(new RuntimeException(errorMessage));

        RequestOrder request = new RequestOrder();
        request.setOrderId(orderId);
        request.setAmount(21500L);

        assertThrows(RuntimeException.class, () -> {
            payService.confirmPayment(request);
        });
    }

    @Test
    @DisplayName("결제 성공 시 모든 필드가 토스 응답값과 일치하게 저장되어야 한다")
    void shouldMapAllFieldsCorrectlyAfterSuccess() {
        String orderId = "order_map_test";
        PayEntity payEntity = PayEntity.processPay(1L, orderId, 21500L);
        when(payRepository.findByOrderId(orderId)).thenReturn(Optional.of(payEntity));

        ResponseOrder mockResponse = new ResponseOrder();
        mockResponse.setPaymentKey("TOSS_KEY_999");
        mockResponse.setMethod("간편결제");
        mockResponse.setVat(100L);
        mockResponse.setApprovedAt(LocalDateTime.parse("2026-04-07T16:30:00"));

        ResponseOrder.ResponseToss card = new ResponseOrder.ResponseToss();
        card.setNumber("1234-5678");
        mockResponse.setCard(card);

        when(tossPayClient.confirm(any())).thenReturn(mockResponse);

        RequestOrder request = new RequestOrder();
        request.setOrderId(orderId);
        request.setAmount(21500L);
        payService.confirmPayment(request);
    }
}