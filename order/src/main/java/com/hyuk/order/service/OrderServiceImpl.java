package com.hyuk.order.service;

import com.hyuk.common.Snowflake;
import com.hyuk.order.dto.OrderRequestDto;
import com.hyuk.order.dto.OrderResponseDto;
import com.hyuk.order.entity.OrderEntity;
import com.hyuk.order.entity.OrderItems;
import com.hyuk.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService{
    private final OrderRepository orderRepository;
    private final ModelMapper modelMapper;
    private final Snowflake snowflake;

    @Override
    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto orderRequestDto, String userId) {
        int totalPrice = 0;
        List<OrderItems> orderItemsList = new ArrayList<>();

        for (OrderRequestDto.OrderItemsRequestDto itemDto : orderRequestDto.getOrderItems()) {
            totalPrice += (itemDto.getPrice() * itemDto.getQuantity());

            OrderItems items = OrderItems.create(
                    snowflake.nextId(),
                    itemDto.getMenuId(),
                    itemDto.getMenuName(),
                    itemDto.getPrice(),
                    itemDto.getQuantity()
            );

            orderItemsList.add(items);
        }

        OrderEntity orderEntity = OrderEntity.create(
                snowflake.nextId(),
                userId,
                orderRequestDto.getRestaurantId(),
                totalPrice,
                orderRequestDto.getDeliveryAddress(),
                orderRequestDto.getUserLatitude(),
                orderRequestDto.getUserLongitude(),
                orderItemsList
        );

        orderRepository.save(orderEntity);

        // Todo: 음식점에 있는 메뉴가 맞는가에 대한 검증
        // Todo: 프론트에서 보낸 가격과 실제 메뉴의 가격 비교 검증

        return modelMapper.map(orderEntity, OrderResponseDto.class);
    }
}
