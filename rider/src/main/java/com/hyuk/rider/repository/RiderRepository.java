package com.hyuk.rider.repository;

import com.hyuk.rider.entity.RiderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiderRepository extends JpaRepository<RiderEntity, Long> {
    RiderEntity findByOrderId(String orderId);
}
