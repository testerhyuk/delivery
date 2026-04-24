package com.hyuk.order.entity.enums;

public enum OrderStatus {
    PENDING, // 결제 대기
    PAID, // 결제 완료
    COOKING, // 조리중
    DELIVERING, // 배달중
    COMPLETED, // 완료
    CANCELED, // 취소
    DELIVERY_START // 배달 시작
}
