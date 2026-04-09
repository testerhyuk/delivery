package com.hyuk.pay.repository;

import com.hyuk.pay.entity.PayOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayOutboxRepository extends JpaRepository<PayOutbox, Long> {
}
