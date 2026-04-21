package com.logistics.dashboard.config;

import com.logistics.dashboard.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Data is seeded via database/init-and-seed.sql (Docker entrypoint).
 * This initializer only logs the current row count on startup.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final OrderRepository orderRepository;

    public DataInitializer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        long count = orderRepository.count();
        log.info("Database contains {} orders on startup (seeded via init-and-seed.sql)", count);
    }
}
