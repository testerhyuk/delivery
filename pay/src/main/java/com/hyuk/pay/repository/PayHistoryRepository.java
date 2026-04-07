package com.hyuk.pay.repository;

import com.hyuk.pay.entity.PayHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayHistoryRepository extends JpaRepository<PayHistoryEntity, Long> {
}
