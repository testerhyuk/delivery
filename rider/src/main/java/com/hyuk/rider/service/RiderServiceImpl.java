package com.hyuk.rider.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyuk.common.Snowflake;
import com.hyuk.rider.dto.RequestOrder;
import com.hyuk.rider.dto.ResponseDelivery;
import com.hyuk.rider.dto.ResponseOrder;
import com.hyuk.rider.entity.Menu;
import com.hyuk.rider.entity.RiderEntity;
import com.hyuk.rider.repository.RiderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class RiderServiceImpl implements RiderService {
    private final RiderRepository riderRepository;
    private final Snowflake snowflake;
    private final RiderOutboxService riderOutboxService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void completeDelivery(RequestOrder request) {
        RiderEntity rider = riderRepository.findByOrderId(String.valueOf(request.getId()));

        if (rider == null) {
            throw new RuntimeException("배달 정보를 찾을 수 없습니다.");
        }

        rider.finishedDelivery();

        ResponseOrder response = convertToResponseOrder(rider);

        riderOutboxService.completeDeliveryEvent(response);
    }

    @Override
    public void acceptDelivery(String orderId, String userId) {
        String lockKey = "delivery:lock:" + orderId;
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, userId, 30, TimeUnit.SECONDS);

        if (!Boolean.TRUE.equals(acquired)) {
            throw new RuntimeException("이미 다른 라이더가 수락한 주문입니다.");
        }

        String orderJson = stringRedisTemplate.opsForValue().get("order:cooking:" + orderId);

        if (orderJson == null) {
            throw new RuntimeException("주문 정보를 찾을 수 없습니다.");
        }

        try {
            RequestOrder request = objectMapper.readValue(orderJson, RequestOrder.class);

            List<Menu> menuList = request.getOrderItems().stream()
                    .map(item -> Menu.create(
                            snowflake.nextId(),
                            item.getMenuName(),
                            item.getPrice(),
                            item.getQuantity()
                    ))
                    .toList();

            RiderEntity riderEntity = RiderEntity.create(
                    snowflake.nextId(),
                    String.valueOf(request.getId()),
                    request.getRestaurantId(),
                    request.getDeliveryAddress(),
                    request.getTotalPrice(),
                    menuList
            );

            riderRepository.save(riderEntity);

            stringRedisTemplate.delete("order:cooking:" + orderId);

            ResponseDelivery response = ResponseDelivery.builder()
                    .id(riderEntity.getId())
                    .riderId(riderEntity.getRiderId())
                    .orderId(riderEntity.getOrderId())
                    .restaurantId(riderEntity.getRestaurantId())
                    .totalPrice(riderEntity.getPrice())
                    .deliveryAddress(riderEntity.getDeliveryAddress())
                    .orderItems(riderEntity.getMenuList().stream().map(
                            menu -> ResponseDelivery.ResponseDeliveryOrderItems.builder()
                                    .menuId(menu.getMenuId())
                                    .menuName(menu.getMenuName())
                                    .price(menu.getPrice())
                                    .quantity(menu.getQuantity())
                                    .build()
                    ).toList())
                    .build();

            riderOutboxService.deliveryStartEvent(response);

        } catch (Exception e) {
            throw new RuntimeException("배달 수락 처리 실패: " + e.getMessage());
        }
    }

    private ResponseOrder convertToResponseOrder(RiderEntity entity) {
        return ResponseOrder.builder()
                .id(Long.valueOf(entity.getOrderId()))
                .orderId(entity.getOrderId())
                .restaurantId(entity.getRestaurantId())
                .totalPrice(entity.getPrice())
                .deliveryAddress(entity.getDeliveryAddress())
                .orderItems(entity.getMenuList().stream().map(
                        menu -> ResponseOrder.ResponseOrderItems.builder()
                                .menuId(menu.getId())
                                .menuName(menu.getMenuName())
                                .price(menu.getPrice())
                                .quantity(menu.getQuantity())
                                .build()
                ).toList())
                .build();
    }
}
