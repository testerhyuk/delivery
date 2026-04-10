package com.hyuk.seller.repository;

import com.hyuk.seller.entity.SellerOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerOutboxRepository extends JpaRepository<SellerOutbox, Long> {
}
