package com.hyuk.pay.service;

import com.hyuk.pay.entity.PayHistoryEntity;
import com.hyuk.pay.repository.PayHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PayLogService {
    private final PayHistoryRepository payHistoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFail(Long id, Long payId, String errorMessage, LocalDateTime failedTime) {
        PayHistoryEntity entity = PayHistoryEntity.failPayment(
            id,
            payId,
            errorMessage,
            failedTime
        );

        payHistoryRepository.save(entity);
    }
}
