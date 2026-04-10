package com.hyuk.rider.service;

import com.hyuk.rider.dto.RequestOrder;

public interface RiderService {
    void confirmDelivery(RequestOrder request);
    void completeDelivery(RequestOrder request);
}
