package com.logistics.dashboard.service;

import com.logistics.dashboard.dto.*;
import com.logistics.dashboard.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final OrderRepository orderRepository;

    public KpiResponse getKPIs(LocalDate startDate, LocalDate endDate,
                               List<String> carriers, List<String> regions) {
        log.info("Fetching KPIs for date range {} to {} with carriers {}, regions {}",
                startDate, endDate, carriers, regions);

        // For simplicity, we'll implement filtering logic here
        // In a real implementation, we'd have more sophisticated query building

        Long totalOrders = orderRepository.countOrdersByDateRange(startDate, endDate);
        Long deliveredOrders = orderRepository.countDeliveredOrdersByDateRange(startDate, endDate);
        Long delayedOrders = orderRepository.countDelayedOrdersByDateRange(startDate, endDate);
        Double avgDeliveryDays = orderRepository.averageDeliveryDaysByDateRange(startDate, endDate);

        BigDecimal onTimeRate = BigDecimal.ZERO;
        if (totalOrders > 0 && deliveredOrders > 0) {
            onTimeRate = BigDecimal.valueOf(deliveredOrders)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
        }

        BigDecimal avgDays = avgDeliveryDays != null ?
                BigDecimal.valueOf(avgDeliveryDays).setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        return new KpiResponse(totalOrders, deliveredOrders, delayedOrders, onTimeRate, avgDays);
    }

    public TimeSeriesResponse getOrderVolumeTimeSeries(String granularity,
                                                       LocalDate startDate, LocalDate endDate,
                                                       List<String> carriers, List<String> regions) {
        log.info("Fetching order volume time series with granularity {} from {} to {}",
                granularity, startDate, endDate);

        // For now, use native query. In a real implementation, add carrier/region filtering
        List<Object[]> results = orderRepository.getOrderCountByTimePeriod(granularity, startDate, endDate);

        List<TimeSeriesData> data = results.stream()
                .map(row -> {
                    // Handle different date types returned by DATE_TRUNC
                    Object dateObj = row[0];
                    LocalDate date;
                    if (dateObj instanceof java.sql.Date) {
                        date = ((java.sql.Date) dateObj).toLocalDate();
                    } else if (dateObj instanceof java.sql.Timestamp) {
                        date = ((java.sql.Timestamp) dateObj).toLocalDate();
                    } else {
                        date = LocalDate.parse(dateObj.toString());
                    }
                    Long count = ((Number) row[1]).longValue();
                    return new TimeSeriesData(date, count);
                })
                .collect(Collectors.toList());

        return new TimeSeriesResponse(granularity, data);
    }

    public DeliveryPerformanceResponse getDeliveryPerformance(String granularity,
                                                               LocalDate startDate, LocalDate endDate,
                                                               List<String> carriers, List<String> regions) {
        log.info("Fetching delivery performance with granularity {} from {} to {}",
                granularity, startDate, endDate);

        // Simplified implementation - in reality would need a more complex query
        List<DeliveryPerformance> data = new ArrayList<>();

        // For demo purposes, create dummy data
        // In production, implement proper SQL query
        LocalDate current = startDate;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        while (!current.isAfter(endDate)) {
            String period;
            if ("week".equals(granularity)) {
                period = current.format(DateTimeFormatter.ofPattern("yyyy-'W'ww"));
                current = current.plusWeeks(1);
            } else {
                period = current.format(formatter);
                current = current.plusMonths(1);
            }

            // Placeholder values - would be calculated from database
            data.add(new DeliveryPerformance(period, 100L + (long)(Math.random() * 50),
                    10L + (long)(Math.random() * 20)));
        }

        return new DeliveryPerformanceResponse(granularity, data);
    }

    public CarrierBreakdownResponse getCarrierBreakdown(LocalDate startDate, LocalDate endDate,
                                                         List<String> carriers, List<String> regions) {
        log.info("Fetching carrier breakdown from {} to {}", startDate, endDate);

        List<Object[]> results = orderRepository.getCarrierBreakdown(startDate, endDate);

        List<CarrierBreakdown> data = results.stream()
                .map(row -> {
                    String carrier = (String) row[0];
                    Long totalOrders = ((Number) row[1]).longValue();
                    Long delayedOrders = ((Number) row[2]).longValue();

                    BigDecimal delayRate = BigDecimal.ZERO;
                    if (totalOrders > 0) {
                        delayRate = BigDecimal.valueOf(delayedOrders)
                                .multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
                    }

                    return new CarrierBreakdown(carrier, totalOrders, delayedOrders, delayRate);
                })
                .collect(Collectors.toList());

        return new CarrierBreakdownResponse(data);
    }
}