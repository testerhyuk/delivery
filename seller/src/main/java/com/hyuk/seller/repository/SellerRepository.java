package com.hyuk.seller.repository;

import com.hyuk.seller.entity.SellerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerRepository extends JpaRepository<SellerEntity, Long> {
    SellerEntity findByOrderId(String orderId);
}
