package com.logistics.dashboard.service;

import com.logistics.dashboard.dto.*;
import com.logistics.dashboard.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    private final OrderRepository orderRepository;

    public DashboardService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public KpiResponse getKPIs(LocalDate startDate, LocalDate endDate,
                               List<String> carriers, List<String> regions) {
        log.info("Fetching KPIs for date range {} to {} with carriers {}, regions {}",
                startDate, endDate, carriers, regions);

        // Use filtered queries
        List<String> carrierList = (carriers != null && !carriers.isEmpty()) ? carriers : null;
        List<String> regionList = (regions != null && !regions.isEmpty()) ? regions : null;

        Long totalOrders = orderRepository.countOrdersByDateRangeWithFilters(startDate, endDate, carrierList, regionList);
        Long deliveredOrders = orderRepository.countDeliveredOrdersByDateRangeWithFilters(startDate, endDate, carrierList, regionList);
        Long delayedOrders = orderRepository.countDelayedOrdersByDateRangeWithFilters(startDate, endDate, carrierList, regionList);
        Double avgDeliveryDays = orderRepository.averageDeliveryDaysByDateRangeWithFilters(startDate, endDate, carrierList, regionList);

        BigDecimal onTimeRate = BigDecimal.ZERO;
        if (totalOrders != null && totalOrders > 0 && deliveredOrders != null && deliveredOrders > 0) {
            onTimeRate = BigDecimal.valueOf(deliveredOrders)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
        }

        BigDecimal avgDays = avgDeliveryDays != null ?
                BigDecimal.valueOf(avgDeliveryDays).setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        return new KpiResponse(totalOrders != null ? totalOrders : 0L,
                deliveredOrders != null ? deliveredOrders : 0L,
                delayedOrders != null ? delayedOrders : 0L,
                onTimeRate, avgDays);
    }

    public TimeSeriesResponse getOrderVolumeTimeSeries(String granularity,
                                                       LocalDate startDate, LocalDate endDate,
                                                       List<String> carriers, List<String> regions) {
        log.info("Fetching order volume time series with granularity {} from {} to {} with carriers {}, regions {}",
                granularity, startDate, endDate, carriers, regions);

        List<String> carrierList = (carriers != null && !carriers.isEmpty()) ? carriers : null;
        List<String> regionList = (regions != null && !regions.isEmpty()) ? regions : null;

        List<Object[]> results = orderRepository.getOrderCountByTimePeriodWithFilters(
                granularity, startDate, endDate, carrierList, regionList);

        List<TimeSeriesData> data = results.stream()
                .map(row -> {
                    // Handle different date types returned by DATE_TRUNC
                    Object dateObj = row[0];
                    LocalDate date;
                    if (dateObj instanceof java.sql.Date) {
                        date = ((java.sql.Date) dateObj).toLocalDate();
                    } else if (dateObj instanceof java.sql.Timestamp) {
                        date = ((java.sql.Timestamp) dateObj).toLocalDateTime().toLocalDate();
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
        log.info("Fetching delivery performance with granularity {} from {} to {} with carriers {}, regions {}",
                granularity, startDate, endDate, carriers, regions);

        List<String> carrierList = (carriers != null && !carriers.isEmpty()) ? carriers : null;
        List<String> regionList = (regions != null && !regions.isEmpty()) ? regions : null;

        List<Object[]> results = orderRepository.getDeliveryPerformanceByTimePeriod(
                granularity, startDate, endDate, carrierList, regionList);

        List<DeliveryPerformance> data = results.stream()
                .map(row -> {
                    Object periodObj = row[0];
                    String period;
                    if (periodObj instanceof java.sql.Date) {
                        period = ((java.sql.Date) periodObj).toLocalDate().toString();
                    } else if (periodObj instanceof java.sql.Timestamp) {
                        period = ((java.sql.Timestamp) periodObj).toLocalDateTime().toLocalDate().toString();
                    } else {
                        period = periodObj.toString();
                    }
                    Long onTime = ((Number) row[1]).longValue();
                    Long delayed = ((Number) row[2]).longValue();
                    return new DeliveryPerformance(period, onTime, delayed);
                })
                .collect(Collectors.toList());

        return new DeliveryPerformanceResponse(granularity, data);
    }

    public CarrierBreakdownResponse getCarrierBreakdown(LocalDate startDate, LocalDate endDate,
                                                         List<String> carriers, List<String> regions) {
        log.info("Fetching carrier breakdown from {} to {} with carriers {}, regions {}",
                startDate, endDate, carriers, regions);

        List<String> carrierList = (carriers != null && !carriers.isEmpty()) ? carriers : null;
        List<String> regionList = (regions != null && !regions.isEmpty()) ? regions : null;

        List<Object[]> results = orderRepository.getCarrierBreakdownWithFilters(
                startDate, endDate, carrierList, regionList);

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