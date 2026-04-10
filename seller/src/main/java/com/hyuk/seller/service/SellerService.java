package com.hyuk.seller.service;

import com.hyuk.seller.dto.RequestOrder;

public interface SellerService {
    void confirmOrder(RequestOrder requestOrder);
    void deliveryStarted(String orderId);
}
