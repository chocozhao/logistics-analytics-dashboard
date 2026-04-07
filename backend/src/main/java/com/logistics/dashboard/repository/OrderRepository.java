package com.logistics.dashboard.repository;

import com.logistics.dashboard.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Basic filtered queries
    List<Order> findByOrderDateBetween(LocalDate startDate, LocalDate endDate);

    List<Order> findByCarrierInAndOrderDateBetween(List<String> carriers, LocalDate startDate, LocalDate endDate);

    List<Order> findByDestinationRegionInAndOrderDateBetween(List<String> regions, LocalDate startDate, LocalDate endDate);

    List<Order> findByCarrierInAndDestinationRegionInAndOrderDateBetween(
            List<String> carriers, List<String> regions, LocalDate startDate, LocalDate endDate);

    // Custom queries for analytics
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    Long countOrdersByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'delivered' AND o.orderDate BETWEEN :startDate AND :endDate")
    Long countDeliveredOrdersByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'delivered' " +
           "AND o.actualDeliveryDate > o.promisedDeliveryDate " +
           "AND o.orderDate BETWEEN :startDate AND :endDate")
    Long countDelayedOrdersByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT AVG(DATEDIFF(day, o.orderDate, o.actualDeliveryDate)) " +
           "FROM Order o WHERE o.status = 'delivered' " +
           "AND o.actualDeliveryDate IS NOT NULL " +
           "AND o.orderDate BETWEEN :startDate AND :endDate")
    Double averageDeliveryDaysByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // For time series data - H2 compatible version
    @Query(value = "SELECT " +
           "CASE :granularity " +
           "   WHEN 'day' THEN CAST(o.order_date AS DATE) " +
           "   WHEN 'week' THEN DATEADD('DAY', 1 - MOD(EXTRACT(DAY_OF_WEEK FROM o.order_date) + 5, 7), CAST(o.order_date AS DATE)) " +
           "   WHEN 'month' THEN DATEADD('DAY', 1 - EXTRACT(DAY FROM o.order_date), CAST(o.order_date AS DATE)) " +
           "END as period, " +
           "COUNT(*) as count " +
           "FROM orders o " +
           "WHERE o.order_date BETWEEN :startDate AND :endDate " +
           "GROUP BY " +
           "CASE :granularity " +
           "   WHEN 'day' THEN CAST(o.order_date AS DATE) " +
           "   WHEN 'week' THEN DATEADD('DAY', 1 - MOD(EXTRACT(DAY_OF_WEEK FROM o.order_date) + 5, 7), CAST(o.order_date AS DATE)) " +
           "   WHEN 'month' THEN DATEADD('DAY', 1 - EXTRACT(DAY FROM o.order_date), CAST(o.order_date AS DATE)) " +
           "END " +
           "ORDER BY period", nativeQuery = true)
    List<Object[]> getOrderCountByTimePeriod(
            @Param("granularity") String granularity, // 'day', 'week', 'month'
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Carrier breakdown
    @Query(value = "SELECT o.carrier, COUNT(*) as total_orders, " +
           "COUNT(CASE WHEN o.status = 'delivered' AND o.actual_delivery_date > o.promised_delivery_date THEN 1 END) as delayed_orders " +
           "FROM orders o " +
           "WHERE o.order_date BETWEEN :startDate AND :endDate " +
           "GROUP BY o.carrier " +
           "ORDER BY total_orders DESC", nativeQuery = true)
    List<Object[]> getCarrierBreakdown(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Filtered count queries for KPIs
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate " +
           "AND (:carriers IS NULL OR o.carrier IN :carriers) " +
           "AND (:regions IS NULL OR o.destinationRegion IN :regions)")
    Long countOrdersByDateRangeWithFilters(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("carriers") List<String> carriers,
            @Param("regions") List<String> regions);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'delivered' AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND (:carriers IS NULL OR o.carrier IN :carriers) " +
           "AND (:regions IS NULL OR o.destinationRegion IN :regions)")
    Long countDeliveredOrdersByDateRangeWithFilters(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("carriers") List<String> carriers,
            @Param("regions") List<String> regions);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'delivered' " +
           "AND o.actualDeliveryDate > o.promisedDeliveryDate " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND (:carriers IS NULL OR o.carrier IN :carriers) " +
           "AND (:regions IS NULL OR o.destinationRegion IN :regions)")
    Long countDelayedOrdersByDateRangeWithFilters(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("carriers") List<String> carriers,
            @Param("regions") List<String> regions);

    @Query("SELECT AVG(DATEDIFF(day, o.orderDate, o.actualDeliveryDate)) " +
           "FROM Order o WHERE o.status = 'delivered' " +
           "AND o.actualDeliveryDate IS NOT NULL " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND (:carriers IS NULL OR o.carrier IN :carriers) " +
           "AND (:regions IS NULL OR o.destinationRegion IN :regions)")
    Double averageDeliveryDaysByDateRangeWithFilters(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("carriers") List<String> carriers,
            @Param("regions") List<String> regions);

    // Filtered time series query
    @Query(value = "SELECT " +
           "CASE :granularity " +
           "   WHEN 'day' THEN CAST(o.order_date AS DATE) " +
           "   WHEN 'week' THEN DATEADD('DAY', 1 - MOD(EXTRACT(DAY_OF_WEEK FROM o.order_date) + 5, 7), CAST(o.order_date AS DATE)) " +
           "   WHEN 'month' THEN DATEADD('DAY', 1 - EXTRACT(DAY FROM o.order_date), CAST(o.order_date AS DATE)) " +
           "END as period, " +
           "COUNT(*) as count " +
           "FROM orders o " +
           "WHERE o.order_date BETWEEN :startDate AND :endDate " +
           "AND (:carriers IS NULL OR o.carrier IN (:carriers)) " +
           "AND (:regions IS NULL OR o.destination_region IN (:regions)) " +
           "GROUP BY " +
           "CASE :granularity " +
           "   WHEN 'day' THEN CAST(o.order_date AS DATE) " +
           "   WHEN 'week' THEN DATEADD('DAY', 1 - MOD(EXTRACT(DAY_OF_WEEK FROM o.order_date) + 5, 7), CAST(o.order_date AS DATE)) " +
           "   WHEN 'month' THEN DATEADD('DAY', 1 - EXTRACT(DAY FROM o.order_date), CAST(o.order_date AS DATE)) " +
           "END " +
           "ORDER BY period", nativeQuery = true)
    List<Object[]> getOrderCountByTimePeriodWithFilters(
            @Param("granularity") String granularity,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("carriers") List<String> carriers,
            @Param("regions") List<String> regions);

    // Filtered carrier breakdown
    @Query(value = "SELECT o.carrier, COUNT(*) as total_orders, " +
           "COUNT(CASE WHEN o.status = 'delivered' AND o.actual_delivery_date > o.promised_delivery_date THEN 1 END) as delayed_orders " +
           "FROM orders o " +
           "WHERE o.order_date BETWEEN :startDate AND :endDate " +
           "AND (:carriers IS NULL OR o.carrier IN (:carriers)) " +
           "AND (:regions IS NULL OR o.destination_region IN (:regions)) " +
           "GROUP BY o.carrier " +
           "ORDER BY total_orders DESC", nativeQuery = true)
    List<Object[]> getCarrierBreakdownWithFilters(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("carriers") List<String> carriers,
            @Param("regions") List<String> regions);

    // Delivery performance query (on-time vs delayed by time period)
    @Query(value = "SELECT " +
           "CASE :granularity " +
           "   WHEN 'day' THEN CAST(o.order_date AS DATE) " +
           "   WHEN 'week' THEN DATEADD('DAY', 1 - MOD(EXTRACT(DAY_OF_WEEK FROM o.order_date) + 5, 7), CAST(o.order_date AS DATE)) " +
           "   WHEN 'month' THEN DATEADD('DAY', 1 - EXTRACT(DAY FROM o.order_date), CAST(o.order_date AS DATE)) " +
           "END as period, " +
           "COUNT(CASE WHEN o.status = 'delivered' AND o.actual_delivery_date <= o.promised_delivery_date THEN 1 END) as on_time, " +
           "COUNT(CASE WHEN o.status = 'delivered' AND o.actual_delivery_date > o.promised_delivery_date THEN 1 END) as delayed " +
           "FROM orders o " +
           "WHERE o.order_date BETWEEN :startDate AND :endDate " +
           "AND (:carriers IS NULL OR o.carrier IN (:carriers)) " +
           "AND (:regions IS NULL OR o.destination_region IN (:regions)) " +
           "GROUP BY " +
           "CASE :granularity " +
           "   WHEN 'day' THEN CAST(o.order_date AS DATE) " +
           "   WHEN 'week' THEN DATEADD('DAY', 1 - MOD(EXTRACT(DAY_OF_WEEK FROM o.order_date) + 5, 7), CAST(o.order_date AS DATE)) " +
           "   WHEN 'month' THEN DATEADD('DAY', 1 - EXTRACT(DAY FROM o.order_date), CAST(o.order_date AS DATE)) " +
           "END " +
           "ORDER BY period", nativeQuery = true)
    List<Object[]> getDeliveryPerformanceByTimePeriod(
            @Param("granularity") String granularity,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("carriers") List<String> carriers,
            @Param("regions") List<String> regions);
}