package com.hyuk.rider.service;

import com.hyuk.rider.dto.RequestOrder;

public interface RiderService {
    void completeDelivery(String orderId);
    void acceptDelivery(String orderId, String userId);
    void startDelivery(String orderId);
}
