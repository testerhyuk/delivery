package com.hyuk.order.controller;

import com.hyuk.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderValidationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    @DisplayName("실패 : OrderItems가 비어있으면 400 에러 반환")
    void emptyOrderItems() throws Exception {
        String content = """
                {
                    "restaurantId": "RES-1",
                    "deliveryAddress": "인천시",
                    "userLatitude": 12.11,
                    "userLongitude": 22.22,
                    "orderItems": []
                }
                """;

        mockMvc.perform(post("/order-service/order")
                    .header("X-User-Id", "USER-1234")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(content))
                .andExpect(status().isBadRequest());
    }
}