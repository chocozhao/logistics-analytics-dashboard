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

    // For time series data
    @Query(value = "SELECT DATE_TRUNC(:granularity, o.order_date) as period, COUNT(*) as count " +
           "FROM orders o " +
           "WHERE o.order_date BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE_TRUNC(:granularity, o.order_date) " +
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
}