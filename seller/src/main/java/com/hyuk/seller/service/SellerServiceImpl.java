package com.hyuk.seller.service;

import com.hyuk.common.Snowflake;
import com.hyuk.seller.dto.RequestOrder;
import com.hyuk.seller.dto.ResponseOrder;
import com.hyuk.seller.entity.Menu;
import com.hyuk.seller.entity.SellerEntity;
import com.hyuk.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SellerServiceImpl implements SellerService {
    private final SellerRepository sellerRepository;
    private final Snowflake snowflake;
    private final SellerOutboxService sellerOutboxService;

    @Override
    public void confirmOrder(RequestOrder requestOrder) {
        List<Menu> menuList = new ArrayList<>();

        SellerEntity entity = SellerEntity.create(
                snowflake.nextId(),
                String.valueOf(requestOrder.getId()),
                String.valueOf(requestOrder.getRestaurantId()),
                requestOrder.getDeliveryAddress(),
                requestOrder.getTotalPrice(),
                menuList,
                requestOrder.getOrderAt(),
                null
        );

        requestOrder.getOrderItems().forEach(item -> {
            Menu menu = Menu.create(
                    snowflake.nextId(),
                    item.getMenuName(),
                    item.getPrice(),
                    item.getQuantity()
            );

            entity.addMenu(menu);
        });

        sellerRepository.save(entity);

        ResponseOrder response = convertToResponseOrder(entity);

        sellerOutboxService.saveCookingOrDeliveringEvent(response, "COOKING");
    }

    @Override
    public void deliveryStarted(String orderId) {
        SellerEntity entity = sellerRepository.findByOrderId(orderId);

        ResponseOrder response = convertToResponseOrder(entity);

        sellerOutboxService.saveCookingOrDeliveringEvent(response, "DELIVERING");
    }

    private ResponseOrder convertToResponseOrder(SellerEntity entity) {
        return ResponseOrder.builder()
                .id(Long.valueOf(entity.getOrderId()))
                .restaurantId(Long.valueOf(entity.getRestaurantId()))
                .totalPrice(entity.getPrice())
                .deliveryAddress(entity.getDeliveryAddress())
                .orderAt(entity.getOrderAt())
                .orderItems(entity.getMenu().stream().map(
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
