package com.hyuk.rider.repository;

import com.hyuk.rider.entity.RiderOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiderOutboxRepository extends JpaRepository<RiderOutbox, Long> {
}
