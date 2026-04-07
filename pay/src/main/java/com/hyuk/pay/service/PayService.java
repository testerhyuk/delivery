package com.hyuk.pay.service;


import com.hyuk.pay.dto.RequestOrder;
import com.hyuk.pay.dto.ResponseOrder;

public interface PayService {
    void readyPayment(RequestOrder requestOrder);
    ResponseOrder confirmPayment(RequestOrder requestOrder);
}
