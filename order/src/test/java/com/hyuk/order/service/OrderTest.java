package com.hyuk.order.service;

import com.hyuk.common.Snowflake;
import com.hyuk.order.dto.OrderRequestDto;
import com.hyuk.order.dto.OrderResponseDto;
import com.hyuk.order.entity.OrderEntity;
import com.hyuk.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderTest {
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private Snowflake snowflake;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    private String userId;
    private OrderRequestDto dto;

    @BeforeEach
    void setUp() {
        userId = "USER-1234";

        OrderRequestDto.OrderItemsRequestDto itemDto = OrderRequestDto.OrderItemsRequestDto.builder()
                .menuId("1")
                .menuName("menu")
                .price(1000)
                .quantity(2)
                .build();

        dto = OrderRequestDto.builder()
                .restaurantId("RES-1")
                .deliveryAddress("인천시")
                .userLatitude(BigDecimal.valueOf(12.11))
                .userLongitude(BigDecimal.valueOf(22.22))
                .orderItems(List.of(itemDto))
                .build();
    }

    @Test
    @DisplayName("성공 : 주문시 엔티티 생성 및 저장 검증")
    void createOrder() {
        // given
        when(snowflake.nextId()).thenReturn(1L);

        // when
        OrderResponseDto response = orderService.createOrder(dto, userId);

        // then
        verify(orderRepository, times(1)).save(any(OrderEntity.class));

        assertNotNull(response);
        assertEquals(2000, response.getTotalPrice());
        System.out.println("response : " + response);
    }
}