package com.hyuk.rider.service;

import com.hyuk.rider.dto.RequestOrder;

public interface RiderService {
    void completeDelivery(RequestOrder request);
    void acceptDelivery(String orderId, String userId);
}
