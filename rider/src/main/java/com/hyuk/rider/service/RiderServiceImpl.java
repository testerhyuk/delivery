package com.hyuk.rider.service;

import com.hyuk.common.Snowflake;
import com.hyuk.rider.dto.RequestOrder;
import com.hyuk.rider.dto.ResponseOrder;
import com.hyuk.rider.entity.Menu;
import com.hyuk.rider.entity.RiderEntity;
import com.hyuk.rider.repository.RiderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RiderServiceImpl implements RiderService {
    private final RiderRepository riderRepository;
    private final Snowflake snowflake;
    private final RiderOutboxService riderOutboxService;

    @Override
    public void confirmDelivery(RequestOrder request) {
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
                String.valueOf(request.getRestaurantId()),
                request.getDeliveryAddress(),
                request.getTotalPrice(),
                menuList
        );

        riderRepository.save(riderEntity);
    }

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

    private ResponseOrder convertToResponseOrder(RiderEntity entity) {
        return ResponseOrder.builder()
                .id(Long.valueOf(entity.getOrderId()))
                .restaurantId(Long.valueOf(entity.getRestaurantId()))
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
