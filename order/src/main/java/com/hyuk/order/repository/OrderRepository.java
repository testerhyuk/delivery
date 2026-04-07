package com.hyuk.order.repository;

import com.hyuk.order.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    @Override
    Optional<OrderEntity> findById(Long orderId);
}
