package com.hyuk.pay.service;


import com.hyuk.pay.dto.RequestOrder;
import com.hyuk.pay.dto.ResponseOrder;
import com.hyuk.pay.dto.ResponsePayReady;
import com.hyuk.pay.dto.TossCancelResponse;

public interface PayService {
    ResponsePayReady readyPayment(RequestOrder requestOrder);
    ResponseOrder confirmPayment(RequestOrder requestOrder);
    void cancelPayment(TossCancelResponse response, boolean publishEvent);
    void userCancelProcess(String orderId, String reason);
}
