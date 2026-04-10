package com.hyuk.order.service;

import com.hyuk.common.Snowflake;
import com.hyuk.order.client.PayServiceClient;
import com.hyuk.order.client.RestaurantServiceClient;
import com.hyuk.order.dto.*;
import com.hyuk.order.entity.OrderEntity;
import com.hyuk.order.entity.OrderItems;
import com.hyuk.order.entity.enums.OrderStatus;
import com.hyuk.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService{
    private final OrderRepository orderRepository;
    private final ModelMapper modelMapper;
    private final Snowflake snowflake;
    private final RestaurantServiceClient restaurantServiceClient;
    private final PayServiceClient payServiceClient;
    private final OrderOutboxService orderOutboxService;

    @Override
    public OrderResponseDto createOrder(OrderRequestDto orderRequestDto, String userId) {
        int totalPrice = 0;
        List<OrderItems> orderItemsList = new ArrayList<>();
        List<ResponseMenu> menuBoard = restaurantServiceClient.getMenu(orderRequestDto.getRestaurantId());

        for (OrderRequestDto.OrderItemsRequestDto itemDto : orderRequestDto.getOrderItems()) {
            ResponseMenu actualMenu = menuBoard.stream()
                    .filter(m -> String.valueOf(m.getId()).equals(itemDto.getMenuId())
                            || (m.getMenuId() != null && m.getMenuId().equals(itemDto.getMenuId()))
                            || m.getName().equals(itemDto.getMenuName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("식당에서 판매하지 않는 메뉴가 포함되어 있습니다: " + itemDto.getMenuName()));

            totalPrice += (actualMenu.getPrice() * itemDto.getQuantity());

            OrderItems items = OrderItems.create(
                    snowflake.nextId(),
                    itemDto.getMenuId(),
                    itemDto.getMenuName(),
                    actualMenu.getPrice(),
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

        PayRequestDto payRequestDto = new PayRequestDto();
        payRequestDto.setOrderId(String.valueOf(orderEntity.getId()));
        payRequestDto.setUserId(userId);
        payRequestDto.setRestaurantId(orderEntity.getRestaurantId());
        payRequestDto.setAmount(Long.valueOf(orderEntity.getTotalPrice()));
        payRequestDto.setOrderItems(orderEntity.getOrderItems().stream().map(item -> {
            return modelMapper.map(item, PayRequestDto.RequestOrderItems.class);
        }).toList());

        ResponsePayReady payResponse = payServiceClient.readyPayment(payRequestDto);

        OrderResponseDto orderResponseDto = modelMapper.map(orderEntity, OrderResponseDto.class);
        orderResponseDto.setPaymentInfo(payResponse);

        return orderResponseDto;
    }

    @Override
    public void moneyPaid(PayConfirmedRequestDto payConfirmedRequestDto) {
        try {
            if (!"DONE".equalsIgnoreCase(payConfirmedRequestDto.getPayStatus())) {
                throw new IllegalStateException("결제가 완료되지 않은 주문은 처리할 수 없습니다. 상태: " + payConfirmedRequestDto.getPayStatus());
            }

            OrderEntity entity = orderRepository.findById(Long.valueOf(payConfirmedRequestDto.getOrderId()))
                    .orElseThrow(() -> new RuntimeException("해당 ID의 주문을 찾을 수 없습니다"));

            entity.updateToPaid();

            SellerResponseDto sellerResponse = SellerResponseDto.builder()
                    .id(entity.getId())
                    .userId(entity.getUserId())
                    .restaurantId(entity.getRestaurantId())
                    .orderStatus(entity.getOrderStatus())
                    .totalPrice(entity.getTotalPrice())
                    .deliveryAddress(entity.getDeliveryAddress())
                    .orderAt(entity.getOrderAt())
                    .orderItems(entity.getOrderItems().stream()
                            .map(item -> {
                                SellerResponseDto.ResponseOrderItems itemDto = new SellerResponseDto.ResponseOrderItems();
                                itemDto.setMenuId(item.getMenuId());
                                itemDto.setMenuName(item.getMenuName());
                                itemDto.setPrice(item.getPrice());
                                itemDto.setQuantity(item.getQuantity());
                                return itemDto;
                            })
                            .toList())
                    .build();

            orderOutboxService.saveSendToSellerEvent(payConfirmedRequestDto.getOrderId(), "ORDER", sellerResponse);
        } catch (Exception e) {
            orderOutboxService.saveCancelEvent(payConfirmedRequestDto, "CANCEL");
        }
    }

    @Override
    public void cancelOrder(Long orderId) {
        OrderEntity entity = orderRepository.findById(orderId).orElseThrow(
                () -> new RuntimeException("주문 정보를 찾을 수 없습니다.")
        );

        if (entity.getOrderStatus() == OrderStatus.CANCELED) {
            throw new RuntimeException("이미 취소된 주문입니다.");
        }

        entity.updateToCancelled();
    }

    @Override
    public void updateToCooking(Long orderId) {
        OrderEntity entity = orderRepository.findById(orderId).orElseThrow(
                () -> new RuntimeException("주문 정보를 찾을 수 없습니다.")
        );

        if (entity.getOrderStatus() == OrderStatus.CANCELED) {
            throw new RuntimeException("취소된 주문은 조리를 시작할 수 없습니다.");
        }

        if (entity.getOrderStatus() != OrderStatus.PAID) {
            throw new RuntimeException("결제가 완료되지 않은 주문입니다.");
        }

        entity.updateToCooking();
    }

    @Override
    public void updateToDelivering(Long orderId) {
        OrderEntity entity = orderRepository.findById(orderId).orElseThrow(
                () -> new RuntimeException("주문 정보를 찾을 수 없습니다.")
        );

        if (entity.getOrderStatus() != OrderStatus.COOKING) {
            throw new RuntimeException("현재 조리 중인 주문만 배송 처리가 가능합니다.");
        }

        entity.updateToDelivering();
    }

    @Override
    public OrderCompleteResponseDto completeOrder(Long orderId) {
        OrderEntity entity = orderRepository.findById(orderId).orElseThrow(
                () -> new RuntimeException("주문 정보를 찾을 수 없습니다.")
        );

        if (entity.getOrderStatus() == OrderStatus.COMPLETED) {
            return modelMapper.map(entity, OrderCompleteResponseDto.class);
        }

        if (entity.getOrderStatus() != OrderStatus.DELIVERING) {
            throw new RuntimeException("배송 중인 주문만 완료 처리할 수 있습니다.");
        }

        entity.updateToCompleted();

        orderOutboxService.saveOrderCompleteEvent(String.valueOf(orderId), "COMPLETED");

        return modelMapper.map(entity, OrderCompleteResponseDto.class);
    }
}
