package com.logistics.dashboard.service;

import com.logistics.dashboard.dto.*;
import com.logistics.dashboard.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    private final OrderRepository orderRepository;

    public DashboardService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // Convert list to array for native queries; null means no filter
    private String[] toArray(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return list.toArray(new String[0]);
    }

    public KpiResponse getKPIs(LocalDate startDate, LocalDate endDate,
                               List<String> carriers, List<String> regions) {
        return getKPIs(startDate, endDate, carriers, regions, null);
    }

    public KpiResponse getKPIs(LocalDate startDate, LocalDate endDate,
                               List<String> carriers, List<String> regions, List<String> categories) {
        log.info("Fetching KPIs {} to {} carriers={} regions={} categories={}", startDate, endDate, carriers, regions, categories);

        List<String> c = (carriers != null && !carriers.isEmpty()) ? carriers : null;
        List<String> r = (regions != null && !regions.isEmpty()) ? regions : null;
        List<String> cat = (categories != null && !categories.isEmpty()) ? categories : null;

        Long totalOrders    = orderRepository.countOrdersByDateRangeWithFilters(startDate, endDate, c, r, cat);
        Long deliveredOrders = orderRepository.countDeliveredOrdersByDateRangeWithFilters(startDate, endDate, c, r, cat);
        Long delayedOrders  = orderRepository.countDelayedOrdersByDateRangeWithFilters(startDate, endDate, c, r, cat);
        Double avgDeliveryDays = orderRepository.averageDeliveryDaysByDateRangeWithFilters(
                startDate, endDate, toArray(c), toArray(r), toArray(cat));

        BigDecimal onTimeRate = BigDecimal.ZERO;
        if (totalOrders != null && totalOrders > 0 && deliveredOrders != null) {
            onTimeRate = BigDecimal.valueOf(deliveredOrders)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
        }

        BigDecimal avgDays = avgDeliveryDays != null
                ? BigDecimal.valueOf(avgDeliveryDays).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new KpiResponse(
                totalOrders != null ? totalOrders : 0L,
                deliveredOrders != null ? deliveredOrders : 0L,
                delayedOrders != null ? delayedOrders : 0L,
                onTimeRate, avgDays);
    }

    public TimeSeriesResponse getOrderVolumeTimeSeries(String granularity,
                                                       LocalDate startDate, LocalDate endDate,
                                                       List<String> carriers, List<String> regions) {
        return getOrderVolumeTimeSeries(granularity, startDate, endDate, carriers, regions, null);
    }

    public TimeSeriesResponse getOrderVolumeTimeSeries(String granularity,
                                                       LocalDate startDate, LocalDate endDate,
                                                       List<String> carriers, List<String> regions,
                                                       List<String> categories) {
        log.info("Fetching order volume granularity={} {} to {}", granularity, startDate, endDate);

        List<String> c = (carriers != null && !carriers.isEmpty()) ? carriers : null;
        List<String> r = (regions != null && !regions.isEmpty()) ? regions : null;
        List<String> cat = (categories != null && !categories.isEmpty()) ? categories : null;

        List<Object[]> results = orderRepository.getOrderCountByTimePeriodWithFilters(
                granularity, startDate, endDate, toArray(c), toArray(r), toArray(cat));

        List<TimeSeriesData> data = results.stream()
                .map(row -> {
                    LocalDate date = toLocalDate(row[0]);
                    Long count = ((Number) row[1]).longValue();
                    return new TimeSeriesData(date, count);
                })
                .collect(Collectors.toList());

        return new TimeSeriesResponse(granularity, data);
    }

    public DeliveryPerformanceResponse getDeliveryPerformance(String granularity,
                                                               LocalDate startDate, LocalDate endDate,
                                                               List<String> carriers, List<String> regions) {
        return getDeliveryPerformance(granularity, startDate, endDate, carriers, regions, null);
    }

    public DeliveryPerformanceResponse getDeliveryPerformance(String granularity,
                                                               LocalDate startDate, LocalDate endDate,
                                                               List<String> carriers, List<String> regions,
                                                               List<String> categories) {
        log.info("Fetching delivery performance granularity={} {} to {}", granularity, startDate, endDate);

        List<String> c = (carriers != null && !carriers.isEmpty()) ? carriers : null;
        List<String> r = (regions != null && !regions.isEmpty()) ? regions : null;
        List<String> cat = (categories != null && !categories.isEmpty()) ? categories : null;

        List<Object[]> results = orderRepository.getDeliveryPerformanceByTimePeriod(
                granularity, startDate, endDate, toArray(c), toArray(r), toArray(cat));

        List<DeliveryPerformance> data = results.stream()
                .map(row -> {
                    String period = toLocalDate(row[0]).toString();
                    Long onTime  = ((Number) row[1]).longValue();
                    Long delayed = ((Number) row[2]).longValue();
                    return new DeliveryPerformance(period, onTime, delayed);
                })
                .collect(Collectors.toList());

        return new DeliveryPerformanceResponse(granularity, data);
    }

    public CarrierBreakdownResponse getCarrierBreakdown(LocalDate startDate, LocalDate endDate,
                                                         List<String> carriers, List<String> regions) {
        return getCarrierBreakdown(startDate, endDate, carriers, regions, null);
    }

    public CarrierBreakdownResponse getCarrierBreakdown(LocalDate startDate, LocalDate endDate,
                                                         List<String> carriers, List<String> regions,
                                                         List<String> categories) {
        log.info("Fetching carrier breakdown {} to {}", startDate, endDate);

        List<String> c = (carriers != null && !carriers.isEmpty()) ? carriers : null;
        List<String> r = (regions != null && !regions.isEmpty()) ? regions : null;
        List<String> cat = (categories != null && !categories.isEmpty()) ? categories : null;

        List<Object[]> results = orderRepository.getCarrierBreakdownWithFilters(
                startDate, endDate, toArray(c), toArray(r), toArray(cat));

        List<CarrierBreakdown> data = results.stream()
                .map(row -> {
                    String carrier = (String) row[0];
                    Long totalOrders   = ((Number) row[1]).longValue();
                    Long delayedOrders = ((Number) row[2]).longValue();
                    BigDecimal delayRate = BigDecimal.ZERO;
                    BigDecimal onTimeRate = BigDecimal.ZERO;
                    if (totalOrders > 0) {
                        delayRate = BigDecimal.valueOf(delayedOrders)
                                .multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
                        onTimeRate = BigDecimal.valueOf(100).subtract(delayRate);
                    }
                    return new CarrierBreakdown(carrier, totalOrders, delayedOrders, delayRate, onTimeRate);
                })
                .collect(Collectors.toList());

        return new CarrierBreakdownResponse(data);
    }

    public RegionBreakdownResponse getRegionBreakdown(LocalDate startDate, LocalDate endDate,
                                                      List<String> carriers, List<String> regions) {
        return getRegionBreakdown(startDate, endDate, carriers, regions, null);
    }

    public RegionBreakdownResponse getRegionBreakdown(LocalDate startDate, LocalDate endDate,
                                                      List<String> carriers, List<String> regions,
                                                      List<String> categories) {
        log.info("Fetching region breakdown {} to {}", startDate, endDate);

        List<String> c = (carriers != null && !carriers.isEmpty()) ? carriers : null;
        List<String> r = (regions != null && !regions.isEmpty()) ? regions : null;
        List<String> cat = (categories != null && !categories.isEmpty()) ? categories : null;

        List<Object[]> results = orderRepository.getRegionBreakdownWithFilters(
                startDate, endDate, toArray(c), toArray(r), toArray(cat));

        List<RegionBreakdown> data = results.stream()
                .map(row -> {
                    String region = (String) row[0];
                    Long totalOrders   = ((Number) row[1]).longValue();
                    Long delayedOrders = ((Number) row[2]).longValue();
                    BigDecimal delayRate = BigDecimal.ZERO;
                    BigDecimal onTimeRate = BigDecimal.ZERO;
                    if (totalOrders > 0) {
                        delayRate = BigDecimal.valueOf(delayedOrders)
                                .multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
                        onTimeRate = BigDecimal.valueOf(100).subtract(delayRate);
                    }
                    return new RegionBreakdown(region, totalOrders, delayedOrders, delayRate, onTimeRate);
                })
                .collect(Collectors.toList());

        return new RegionBreakdownResponse(data);
    }

    public CategoryBreakdownResponse getCategoryBreakdown(LocalDate startDate, LocalDate endDate,
                                                           List<String> carriers, List<String> regions) {
        return getCategoryBreakdown(startDate, endDate, carriers, regions, null);
    }

    public CategoryBreakdownResponse getCategoryBreakdown(LocalDate startDate, LocalDate endDate,
                                                           List<String> carriers, List<String> regions,
                                                           List<String> categories) {
        log.info("Fetching category breakdown {} to {}", startDate, endDate);

        List<String> c = (carriers != null && !carriers.isEmpty()) ? carriers : null;
        List<String> r = (regions != null && !regions.isEmpty()) ? regions : null;
        List<String> cat = (categories != null && !categories.isEmpty()) ? categories : null;

        List<Object[]> results = orderRepository.getCategoryBreakdownWithFilters(
                startDate, endDate, toArray(c), toArray(r), toArray(cat));

        List<CategoryBreakdown> data = results.stream()
                .map(row -> {
                    String category = (String) row[0];
                    Long totalOrders   = ((Number) row[1]).longValue();
                    Long delayedOrders = ((Number) row[2]).longValue();
                    BigDecimal delayRate = BigDecimal.ZERO;
                    BigDecimal onTimeRate = BigDecimal.ZERO;
                    if (totalOrders > 0) {
                        delayRate = BigDecimal.valueOf(delayedOrders)
                                .multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
                        onTimeRate = BigDecimal.valueOf(100).subtract(delayRate);
                    }
                    return new CategoryBreakdown(category, totalOrders, delayedOrders, delayRate, onTimeRate);
                })
                .collect(Collectors.toList());

        return new CategoryBreakdownResponse(data);
    }

    private LocalDate toLocalDate(Object obj) {
        if (obj instanceof java.sql.Date)      return ((java.sql.Date) obj).toLocalDate();
        if (obj instanceof java.sql.Timestamp) return ((java.sql.Timestamp) obj).toLocalDateTime().toLocalDate();
        return LocalDate.parse(obj.toString());
    }
}
