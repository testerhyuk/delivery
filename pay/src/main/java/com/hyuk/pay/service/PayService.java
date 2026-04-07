package com.hyuk.pay.service;


import com.hyuk.pay.dto.RequestOrder;
import com.hyuk.pay.dto.ResponseOrder;
import com.hyuk.pay.dto.ResponsePayReady;

public interface PayService {
    ResponsePayReady readyPayment(RequestOrder requestOrder);
    ResponseOrder confirmPayment(RequestOrder requestOrder);
}
