package com.hyuk.pay.repository;

import com.hyuk.pay.entity.PayEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayRepository extends JpaRepository<PayEntity, Long> {
    Optional<PayEntity> findByOrderId(String orderId);
}
