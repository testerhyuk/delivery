package com.hyuk.pay.repository;

import com.hyuk.pay.entity.PayEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayRepository extends JpaRepository<PayEntity, Long> {
    PayEntity findByOrderId(String orderId);
}
