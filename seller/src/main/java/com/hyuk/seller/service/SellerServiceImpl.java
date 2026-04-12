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

        String businessOrderId = requestOrder.getOrderId();
        if (businessOrderId == null || businessOrderId.isBlank()) {
            businessOrderId = String.valueOf(requestOrder.getId());
        }

        SellerEntity entity = SellerEntity.create(
                snowflake.nextId(),
                businessOrderId,
                requestOrder.getRestaurantId(),
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
    }

    @Override
    public void deliveryStarted(String orderId) {
        SellerEntity entity = sellerRepository.findByOrderId(orderId);
        if (entity == null && orderId != null && orderId.matches("\\d+")) {
            entity = sellerRepository.findByOrderId("order_" + orderId);
        }
        if (entity == null) {
            throw new RuntimeException("주문을 찾을 수 없습니다: " + orderId);
        }

        ResponseOrder response = convertToResponseOrder(entity);

        sellerOutboxService.saveCookingOrDeliveringEvent(response, "COOKING");
    }

    private ResponseOrder convertToResponseOrder(SellerEntity entity) {
        String oid = entity.getOrderId();
        long numericOrderPk = parseNumericOrderPk(oid);
        return ResponseOrder.builder()
                .id(numericOrderPk)
                .orderId(oid)
                .restaurantId(entity.getRestaurantId())
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

    private static long parseNumericOrderPk(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId is blank");
        }
        int u = orderId.lastIndexOf('_');
        String tail = u >= 0 ? orderId.substring(u + 1) : orderId;
        return Long.parseLong(tail);
    }
}
