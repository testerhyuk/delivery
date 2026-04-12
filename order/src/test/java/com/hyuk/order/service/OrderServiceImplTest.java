package com.hyuk.order.service;

import com.hyuk.common.Snowflake;
import com.hyuk.order.client.PayServiceClient;
import com.hyuk.order.client.RestaurantServiceClient;
import com.hyuk.order.dto.*;
import com.hyuk.order.entity.OrderEntity;
import com.hyuk.order.entity.enums.OrderStatus;
import com.hyuk.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    @Mock
    private OrderRepository orderRepository;
    @InjectMocks
    private OrderServiceImpl orderServiceImpl;

    @Mock
    private RestaurantServiceClient restaurantService;

    @Mock
    private Snowflake snowflake;

    @Mock
    private PayServiceClient payServiceClient;

    @Spy
    private ModelMapper modelMapper;

    @Test
    @DisplayName("주문 정상적으로 생성")
    void createOrder() {
        // 1. Given
        String restaurantId = "1";
        String userId = "user-123";
        Long generatedOrderId = 333L; // Snowflake가 줄 ID

        OrderRequestDto.OrderItemsRequestDto itemRequest = OrderRequestDto.OrderItemsRequestDto.builder()
                .menuId("111").menuName("불고기").quantity(2).build();

        OrderRequestDto request = OrderRequestDto.builder()
                .restaurantId(restaurantId).deliveryAddress("인천시")
                .userLatitude(BigDecimal.valueOf(11.11)).userLongitude(BigDecimal.valueOf(11.11))
                .orderItems(List.of(itemRequest)).build();

        ResponseMenu responseMenu = new ResponseMenu();
        responseMenu.setId(111L);
        responseMenu.setPrice(10000); // 10,000원
        responseMenu.setName("불고기");

        given(restaurantService.getMenu(restaurantId)).willReturn(List.of(responseMenu));
        given(snowflake.nextId()).willReturn(generatedOrderId);

        ResponsePayReady mockPayResponse = new ResponsePayReady();
        mockPayResponse.setOrderId("order_444");
        given(payServiceClient.readyPayment(any(PayRequestDto.class))).willReturn(mockPayResponse);

        // 2. When (실행)
        OrderResponseDto result = orderServiceImpl.createOrder(request, userId);

        // 3. Then
        assertNotNull(result);
        // Snowflake에서 준 ID가 최종 결과에 반영되었는지 검증
        assertEquals(generatedOrderId, result.getId());
        // 가격 계산 검증 (10,000원 * 2개 = 20,000원)
        assertEquals(20000, result.getTotalPrice());
        // 주소 검증
        assertEquals("인천시", result.getDeliveryAddress());
        // 결제 정보 연동 검증
        assertEquals("order_444", result.getPaymentInfo().getOrderId());

        // 서비스 간 협력 검증
        verify(restaurantService, times(1)).getMenu(String.valueOf(restaurantId));
        verify(payServiceClient, times(1)).readyPayment(any(PayRequestDto.class));
        verify(orderRepository, times(1)).save(any(OrderEntity.class));
    }

    @Test
    @DisplayName("결제 완료 처리: 정상 케이스 (DONE 상태)")
    void moneyPaid_Success() {
        Long orderId = 333L;

        OrderEntity mockOrder = OrderEntity.create(
                orderId,
                "user-123",
                "1",
                20000,
                "인천시",
                BigDecimal.valueOf(11.11),
                BigDecimal.valueOf(11.11),
                new ArrayList<>()
        );

        PayConfirmedRequestDto payRequest = new PayConfirmedRequestDto();
        payRequest.setOrderId(String.valueOf(orderId));
        payRequest.setPayStatus("DONE");

        given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));

        orderServiceImpl.moneyPaid(payRequest);

        assertEquals(OrderStatus.PAID, mockOrder.getOrderStatus());
        verify(orderRepository).findById(orderId);
    }

    @Test
    @DisplayName("결제 완료 처리 실패: 결제 상태가 DONE이 아닌 경우")
    void moneyPaid_Fail_NotDone() {
        PayConfirmedRequestDto payRequest = new PayConfirmedRequestDto();
        payRequest.setPayStatus("CANCELED");

        assertThrows(IllegalStateException.class, () -> {
            orderServiceImpl.moneyPaid(payRequest);
        });

        verify(orderRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("주문 취소: 정상 케이스 (PENDING -> CANCELED)")
    void cancelOrder_Success() {
        Long orderId = 777L;

        OrderEntity mockOrder = OrderEntity.create(
                orderId, "user-1", "1", 10000, "인천",
                BigDecimal.valueOf(11.1), BigDecimal.valueOf(11.1), new ArrayList<>()
        );

        given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));

        orderServiceImpl.cancelOrder(orderId);

        assertEquals(OrderStatus.CANCELED, mockOrder.getOrderStatus());
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("주문 취소 실패: 이미 취소된 상태인 경우")
    void cancelOrder_Fail_AlreadyCanceled() {
        Long orderId = 888L;
        OrderEntity mockOrder = OrderEntity.create(
                orderId, "user-1", "1", 10000, "인천",
                BigDecimal.valueOf(11.1), BigDecimal.valueOf(11.1), new ArrayList<>()
        );

        mockOrder.updateToCancelled();

        given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderServiceImpl.cancelOrder(orderId);
        });

        assertEquals("이미 취소된 주문입니다.", exception.getMessage());
    }

    @Test
    @DisplayName("조리 시작: 정상 케이스 (PAID -> COOKING)")
    void updateToCooking_Success() {
        Long orderId = 101L;
        OrderEntity mockOrder = OrderEntity.create(
                orderId, "user-1", "1", 15000, "인천",
                BigDecimal.valueOf(11.1), BigDecimal.valueOf(11.1), new ArrayList<>()
        );

        mockOrder.updateToPaid();

        given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));

        orderServiceImpl.updateToCooking(orderId);

        assertEquals(OrderStatus.COOKING, mockOrder.getOrderStatus());
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("조리 시작 실패: 취소된 주문인 경우")
    void updateToCooking_Fail_Canceled() {
        Long orderId = 102L;
        OrderEntity mockOrder = OrderEntity.create(
                orderId, "user-1", "1", 15000, "인천",
                BigDecimal.valueOf(11.1), BigDecimal.valueOf(11.1), new ArrayList<>()
        );

        mockOrder.updateToCancelled();

        given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderServiceImpl.updateToCooking(orderId);
        });

        assertEquals("취소된 주문은 조리를 시작할 수 없습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("조리 시작 실패: 결제가 완료되지 않은 경우 (PENDING 상태)")
    void updateToCooking_Fail_NotPaid() {
        Long orderId = 103L;
        OrderEntity mockOrder = OrderEntity.create(
                orderId, "user-1", "1", 15000, "인천",
                BigDecimal.valueOf(11.1), BigDecimal.valueOf(11.1), new ArrayList<>()
        );

        given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderServiceImpl.updateToCooking(orderId);
        });

        assertEquals("결제가 완료되지 않은 주문입니다.", exception.getMessage());
    }

    @Test
    @DisplayName("배송 시작: 정상 케이스 (COOKING -> DELIVERING)")
    void updateToDelivering_Success() {
        Long orderId = 201L;
        OrderEntity mockOrder = OrderEntity.create(
                orderId, "user-1", "1", 15000, "인천",
                BigDecimal.valueOf(11.1), BigDecimal.valueOf(11.1), new ArrayList<>()
        );

        mockOrder.updateToPaid();
        mockOrder.updateToCooking();

        given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));

        orderServiceImpl.updateToDelivering(orderId);

        assertEquals(OrderStatus.DELIVERING, mockOrder.getOrderStatus());
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("배송 시작 실패: 조리 중 상태가 아닌 경우 (PAID 상태)")
    void updateToDelivering_Fail_NotCooking() {
        Long orderId = 202L;
        OrderEntity mockOrder = OrderEntity.create(
                orderId, "user-1", "1", 15000, "인천",
                BigDecimal.valueOf(11.1), BigDecimal.valueOf(11.1), new ArrayList<>()
        );

        mockOrder.updateToPaid();

        given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderServiceImpl.updateToDelivering(orderId);
        });

        assertEquals("현재 조리 중인 주문만 배송 처리가 가능합니다.", exception.getMessage());
    }

    @Test
    @DisplayName("배송 완료: 정상 케이스 (DELIVERING -> COMPLETED)")
    void completeOrder_Success() {
        Long orderId = 301L;
        OrderEntity mockOrder = OrderEntity.create(
                orderId, "user-1", "1", 15000, "인천",
                BigDecimal.valueOf(11.1), BigDecimal.valueOf(11.1), new ArrayList<>()
        );

        mockOrder.updateToPaid();
        mockOrder.updateToCooking();
        mockOrder.updateToDelivering();

        given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));

        OrderCompleteResponseDto result = orderServiceImpl.completeOrder(orderId);

        assertNotNull(result);
        assertEquals(OrderStatus.COMPLETED, mockOrder.getOrderStatus());
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("배송 완료: 멱등성 확인 (이미 COMPLETED인 경우 에러 없이 응답)")
    void completeOrder_Idempotency() {
        Long orderId = 302L;
        OrderEntity mockOrder = OrderEntity.create(
                orderId, "user-1", "1", 15000, "인천",
                BigDecimal.valueOf(11.1), BigDecimal.valueOf(11.1), new ArrayList<>()
        );

        mockOrder.updateToPaid();
        mockOrder.updateToCooking();
        mockOrder.updateToDelivering();
        mockOrder.updateToCompleted();

        given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));

        OrderCompleteResponseDto result = orderServiceImpl.completeOrder(orderId);

        assertNotNull(result);
        assertEquals(OrderStatus.COMPLETED, mockOrder.getOrderStatus());
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("배송 완료 실패: 배송 중 상태가 아닌 경우 (COOKING 상태)")
    void completeOrder_Fail_NotDelivering() {
        Long orderId = 303L;
        OrderEntity mockOrder = OrderEntity.create(
                orderId, "user-1", "1", 15000, "인천",
                BigDecimal.valueOf(11.1), BigDecimal.valueOf(11.1), new ArrayList<>()
        );

        mockOrder.updateToPaid();
        mockOrder.updateToCooking();

        given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderServiceImpl.completeOrder(orderId);
        });

        assertEquals("배송 중인 주문만 완료 처리할 수 있습니다.", exception.getMessage());
    }
}