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

    @Query(value = "SELECT AVG(o.actual_delivery_date - o.order_date) " +
           "FROM orders o WHERE o.status = 'delivered' " +
           "AND o.actual_delivery_date IS NOT NULL " +
           "AND o.order_date BETWEEN ?1 AND ?2", nativeQuery = true)
    Double averageDeliveryDaysByDateRange(LocalDate startDate, LocalDate endDate);

    // For time series data - PostgreSQL compatible version
    @Query(value = "SELECT period, COUNT(*) as count FROM (" +
           "SELECT " +
           "CASE ?1 " +
           "   WHEN 'day' THEN o.order_date::date " +
           "   WHEN 'week' THEN date_trunc('week', o.order_date)::date " +
           "   WHEN 'month' THEN date_trunc('month', o.order_date)::date " +
           "END as period " +
           "FROM orders o " +
           "WHERE o.order_date BETWEEN ?2 AND ?3 " +
           ") subquery " +
           "GROUP BY period " +
           "ORDER BY period", nativeQuery = true)
    List<Object[]> getOrderCountByTimePeriod(
            String granularity, // 'day', 'week', 'month'
            LocalDate startDate,
            LocalDate endDate);

    // Carrier breakdown
    @Query(value = "SELECT o.carrier, COUNT(*) as total_orders, " +
           "COUNT(CASE WHEN o.status = 'delivered' AND o.actual_delivery_date > o.promised_delivery_date THEN 1 END) as delayed_orders " +
           "FROM orders o " +
           "WHERE o.order_date BETWEEN ?1 AND ?2 " +
           "GROUP BY o.carrier " +
           "ORDER BY total_orders DESC", nativeQuery = true)
    List<Object[]> getCarrierBreakdown(
            LocalDate startDate, // ?1
            LocalDate endDate); // ?2

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

    @Query(value = "SELECT AVG(o.actual_delivery_date - o.order_date) " +
           "FROM orders o WHERE o.status = 'delivered' " +
           "AND o.actual_delivery_date IS NOT NULL " +
           "AND o.order_date BETWEEN ?1 AND ?2 " +
           "AND (CAST(?3 AS text[]) IS NULL OR o.carrier = ANY(CAST(?3 AS text[]))) " +
           "AND (CAST(?4 AS text[]) IS NULL OR o.destination_region = ANY(CAST(?4 AS text[])))", nativeQuery = true)
    Double averageDeliveryDaysByDateRangeWithFilters(
            LocalDate startDate,
            LocalDate endDate,
            List<String> carriers,
            List<String> regions);

    // Filtered time series query - PostgreSQL compatible
    @Query(value = "SELECT period, COUNT(*) as count FROM (" +
           "SELECT " +
           "CASE ?1 " +
           "   WHEN 'day' THEN o.order_date::date " +
           "   WHEN 'week' THEN date_trunc('week', o.order_date)::date " +
           "   WHEN 'month' THEN date_trunc('month', o.order_date)::date " +
           "END as period " +
           "FROM orders o " +
           "WHERE o.order_date BETWEEN ?2 AND ?3 " +
           "AND (CAST(?4 AS text[]) IS NULL OR o.carrier = ANY(CAST(?4 AS text[]))) " +
           "AND (CAST(?5 AS text[]) IS NULL OR o.destination_region = ANY(CAST(?5 AS text[]))) " +
           ") subquery " +
           "GROUP BY period " +
           "ORDER BY period", nativeQuery = true)
    List<Object[]> getOrderCountByTimePeriodWithFilters(
            String granularity, // ?1
            LocalDate startDate, // ?2
            LocalDate endDate, // ?3
            List<String> carriers, // ?4
            List<String> regions); // ?5

    // Filtered carrier breakdown
    @Query(value = "SELECT o.carrier, COUNT(*) as total_orders, " +
           "COUNT(CASE WHEN o.status = 'delivered' AND o.actual_delivery_date > o.promised_delivery_date THEN 1 END) as delayed_orders " +
           "FROM orders o " +
           "WHERE o.order_date BETWEEN ?1 AND ?2 " +
           "AND (CAST(?3 AS text[]) IS NULL OR o.carrier = ANY(CAST(?3 AS text[]))) " +
           "AND (CAST(?4 AS text[]) IS NULL OR o.destination_region = ANY(CAST(?4 AS text[]))) " +
           "GROUP BY o.carrier " +
           "ORDER BY total_orders DESC", nativeQuery = true)
    List<Object[]> getCarrierBreakdownWithFilters(
            LocalDate startDate, // ?1
            LocalDate endDate, // ?2
            List<String> carriers, // ?3
            List<String> regions); // ?4

    // Delivery performance query (on-time vs delayed by time period) - PostgreSQL compatible
    @Query(value = "SELECT period, " +
           "SUM(CASE WHEN status = 'delivered' AND actual_delivery_date <= promised_delivery_date THEN 1 ELSE 0 END) as on_time, " +
           "SUM(CASE WHEN status = 'delivered' AND actual_delivery_date > promised_delivery_date THEN 1 ELSE 0 END) as delayed " +
           "FROM (" +
           "SELECT " +
           "CASE ?1 " +
           "   WHEN 'day' THEN o.order_date::date " +
           "   WHEN 'week' THEN date_trunc('week', o.order_date)::date " +
           "   WHEN 'month' THEN date_trunc('month', o.order_date)::date " +
           "END as period, " +
           "o.status, " +
           "o.actual_delivery_date, " +
           "o.promised_delivery_date " +
           "FROM orders o " +
           "WHERE o.order_date BETWEEN ?2 AND ?3 " +
           "AND (CAST(?4 AS text[]) IS NULL OR o.carrier = ANY(CAST(?4 AS text[]))) " +
           "AND (CAST(?5 AS text[]) IS NULL OR o.destination_region = ANY(CAST(?5 AS text[]))) " +
           ") subquery " +
           "GROUP BY period " +
           "ORDER BY period", nativeQuery = true)
    List<Object[]> getDeliveryPerformanceByTimePeriod(
            String granularity,
            LocalDate startDate,
            LocalDate endDate,
            List<String> carriers,
            List<String> regions);
}