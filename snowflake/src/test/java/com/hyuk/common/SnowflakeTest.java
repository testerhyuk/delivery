package com.hyuk.common;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SnowflakeTest {
    @Test
    void testIdOrdering() {
        Snowflake snowflake = new Snowflake();

        Long id_one = snowflake.nextId();
        Long id_two = snowflake.nextId();

        assertThat(id_two).isGreaterThan(id_one);
    }

    @Test
    void testMultiThreadUniqueness() throws InterruptedException {
        int threadCount = 100;
        int iterations = 1000;
        Snowflake generator = new Snowflake();
        Set<Long> ids = Collections.synchronizedSet(new HashSet<>());
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                for (int j = 0; j < iterations; j++) {
                    ids.add(generator.nextId());
                }
                latch.countDown();
            });
        }
        latch.await();

        assertThat(ids).hasSize(threadCount * iterations);
    }
}