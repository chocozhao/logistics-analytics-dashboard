package com.logistics.dashboard.config;

import com.logistics.dashboard.entity.Order;
import com.logistics.dashboard.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final OrderRepository orderRepository;

    public DataInitializer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Check if data already exists
        long count = orderRepository.count();
        if (count > 0) {
            log.info("Database already contains {} orders, skipping data initialization", count);
            return;
        }

        log.info("Initializing database with 10000 random orders...");

        Random random = ThreadLocalRandom.current();
        List<String> carriers = Arrays.asList("UPS", "FedEx", "DHL", "USPS", "Amazon Logistics", "Regional Carrier");
        double[] carrierWeights = {0.35, 0.60, 0.80, 0.90, 0.95, 1.0};
        List<String> cities = Arrays.asList("New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "Philadelphia",
                "San Antonio", "San Diego", "Dallas", "San Jose", "Austin", "Jacksonville",
                "Fort Worth", "Columbus", "Charlotte", "San Francisco", "Indianapolis",
                "Seattle", "Denver", "Boston");
        List<Order> orders = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 5);
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        // Forecast range: Oct 1, 2024 to Mar 31, 2025 (ensure data exists for forecasting)
        LocalDate forecastStart = LocalDate.of(2024, 10, 1);
        LocalDate forecastEnd = LocalDate.of(2025, 3, 31);
        long forecastDays = java.time.temporal.ChronoUnit.DAYS.between(forecastStart, forecastEnd);

        for (int i = 0; i < 10000; i++) {
            Order order = new Order();
            // Random order date between startDate and endDate
            // 30% chance to place order in forecast range (Oct 1, 2024 - Mar 31, 2025)
            // to ensure forecast functionality has data
            LocalDate orderDate;
            if (random.nextDouble() < 0.3) {
                // Generate date within forecast range
                long randomForecastDays = (long) (random.nextDouble() * forecastDays);
                orderDate = forecastStart.plusDays(randomForecastDays);
            } else {
                // Generate date uniformly across entire range
                long randomDays = (long) (random.nextDouble() * daysBetween);
                orderDate = startDate.plusDays(randomDays);
            }
            order.setOrderDate(orderDate);

            // Promised delivery 2-7 days after order date
            int promisedOffset = 2 + random.nextInt(6); // 2-7 inclusive
            LocalDate promisedDate = orderDate.plusDays(promisedOffset);
            order.setPromisedDeliveryDate(promisedDate);

            // Status and actual delivery
            double statusRand = random.nextDouble();
            if (statusRand < 0.85) {
                // delivered
                order.setStatus("delivered");
                // actual delivery may be on time or delayed
                int actualOffset = promisedOffset;
                if (random.nextDouble() < 0.2) {
                    // delayed by 1-3 days
                    actualOffset += 1 + random.nextInt(3);
                }
                LocalDate actualDate = orderDate.plusDays(actualOffset);
                order.setActualDeliveryDate(actualDate);
            } else if (statusRand < 0.95) {
                // in_transit
                order.setStatus("in_transit");
                order.setActualDeliveryDate(null);
            } else if (statusRand < 0.98) {
                // pending
                order.setStatus("pending");
                order.setActualDeliveryDate(null);
            } else {
                // cancelled
                order.setStatus("cancelled");
                order.setActualDeliveryDate(null);
            }

            // Carrier weighted selection
            double carrierRand = random.nextDouble();
            String carrier = "UPS";
            for (int j = 0; j < carrierWeights.length; j++) {
                if (carrierRand < carrierWeights[j]) {
                    carrier = carriers.get(j);
                    break;
                }
            }
            order.setCarrier(carrier);

            // Random city
            String city = cities.get(random.nextInt(cities.size()));
            order.setDestinationCity(city);

            // Determine state based on city (simplified)
            String state = "NY";
            if (city.equals("New York")) state = "NY";
            else if (city.equals("Los Angeles") || city.equals("San Diego") || city.equals("San Jose") || city.equals("San Francisco")) state = "CA";
            else if (city.equals("Chicago")) state = "IL";
            else if (city.equals("Houston") || city.equals("Dallas") || city.equals("San Antonio") || city.equals("Austin") || city.equals("Fort Worth")) state = "TX";
            else if (city.equals("Phoenix")) state = "AZ";
            else if (city.equals("Philadelphia")) state = "PA";
            else if (city.equals("Jacksonville")) state = "FL";
            else if (city.equals("Columbus")) state = "OH";
            else if (city.equals("Charlotte")) state = "NC";
            else if (city.equals("Indianapolis")) state = "IN";
            else if (city.equals("Seattle")) state = "WA";
            else if (city.equals("Denver")) state = "CO";
            else if (city.equals("Boston")) state = "MA";
            else state = "CA"; // fallback
            order.setDestinationState(state);

            // Region based on state
            String region = "Other";
            if (state.equals("NY") || state.equals("NJ") || state.equals("PA") || state.equals("CT") || state.equals("MA") || state.equals("RI") || state.equals("NH") || state.equals("VT") || state.equals("ME")) {
                region = "Northeast";
            } else if (state.equals("CA") || state.equals("OR") || state.equals("WA") || state.equals("NV") || state.equals("AZ") || state.equals("UT") || state.equals("CO") || state.equals("NM") || state.equals("WY") || state.equals("MT") || state.equals("ID")) {
                region = "West";
            } else if (state.equals("TX") || state.equals("OK") || state.equals("AR") || state.equals("LA") || state.equals("MS") || state.equals("AL") || state.equals("GA") || state.equals("FL") || state.equals("SC") || state.equals("NC") || state.equals("TN") || state.equals("KY")) {
                region = "South";
            } else if (state.equals("IL") || state.equals("IN") || state.equals("OH") || state.equals("MI") || state.equals("WI") || state.equals("MN") || state.equals("IA") || state.equals("MO") || state.equals("KS") || state.equals("NE") || state.equals("SD") || state.equals("ND")) {
                region = "Midwest";
            }
            order.setDestinationRegion(region);

            // Order value: $50-$5000, skewed toward lower values
            double value = 50 + (random.nextDouble() * 4950 * (1 - Math.pow(random.nextDouble(), 2)));
            order.setOrderValue(BigDecimal.valueOf(value).setScale(2, BigDecimal.ROUND_HALF_UP));

            // SKU
            String sku = "SKU-" + String.format("%04d", 1000 + random.nextInt(9000));
            order.setSku(sku);

            // Quantity: 1-50
            order.setQuantity(1 + random.nextInt(50));

            orders.add(order);
        }

        orderRepository.saveAll(orders);
        log.info("Inserted {} random orders into database", orders.size());
    }

    private Order createOrder(String orderDate, String promisedDate, String actualDate,
                              String status, String carrier, String city, String state,
                              String region, double value, String sku, int quantity) {
        Order order = new Order();
        order.setOrderDate(LocalDate.parse(orderDate));
        order.setPromisedDeliveryDate(LocalDate.parse(promisedDate));
        if (actualDate != null) {
            order.setActualDeliveryDate(LocalDate.parse(actualDate));
        }
        order.setStatus(status);
        order.setCarrier(carrier);
        order.setDestinationCity(city);
        order.setDestinationState(state);
        order.setDestinationRegion(region);
        order.setOrderValue(BigDecimal.valueOf(value));
        order.setSku(sku);
        order.setQuantity(quantity);
        return order;
    }
}