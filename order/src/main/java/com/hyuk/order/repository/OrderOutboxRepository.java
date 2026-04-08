package com.hyuk.order.repository;

import com.hyuk.order.entity.OrderOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, Long> {
}
