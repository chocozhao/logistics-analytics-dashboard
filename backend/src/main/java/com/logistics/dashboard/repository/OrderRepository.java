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

    // ── Count queries ─────────────────────────────────────────────────────────

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    Long countOrdersByDateRange(@Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'delivered' AND o.orderDate BETWEEN :startDate AND :endDate")
    Long countDeliveredOrdersByDateRange(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.deliveryDate > o.promisedDeliveryDate AND o.orderDate BETWEEN :startDate AND :endDate")
    Long countDelayedOrdersByDateRange(@Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE o.orderDate BETWEEN :startDate AND :endDate
        AND (:carriers IS NULL OR o.carrier IN :carriers)
        AND (:regions IS NULL OR o.region IN :regions)
        AND (:categories IS NULL OR o.productCategory IN :categories)
        """)
    Long countOrdersByDateRangeWithFilters(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("carriers") List<String> carriers,
                                           @Param("regions") List<String> regions,
                                           @Param("categories") List<String> categories);

    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE o.status = 'delivered'
        AND o.orderDate BETWEEN :startDate AND :endDate
        AND (:carriers IS NULL OR o.carrier IN :carriers)
        AND (:regions IS NULL OR o.region IN :regions)
        AND (:categories IS NULL OR o.productCategory IN :categories)
        """)
    Long countDeliveredOrdersByDateRangeWithFilters(@Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate,
                                                     @Param("carriers") List<String> carriers,
                                                     @Param("regions") List<String> regions,
                                                     @Param("categories") List<String> categories);

    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE o.deliveryDate > o.promisedDeliveryDate
        AND o.orderDate BETWEEN :startDate AND :endDate
        AND (:carriers IS NULL OR o.carrier IN :carriers)
        AND (:regions IS NULL OR o.region IN :regions)
        AND (:categories IS NULL OR o.productCategory IN :categories)
        """)
    Long countDelayedOrdersByDateRangeWithFilters(@Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate,
                                                   @Param("carriers") List<String> carriers,
                                                   @Param("regions") List<String> regions,
                                                   @Param("categories") List<String> categories);

    // ── Average delivery days ─────────────────────────────────────────────────

    @Query(value = """
        SELECT AVG(o.delivery_date - o.order_date)
        FROM orders o
        WHERE o.delivery_date IS NOT NULL
        AND o.order_date BETWEEN :startDate AND :endDate
        """, nativeQuery = true)
    Double averageDeliveryDaysByDateRange(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    @Query(value = """
        SELECT AVG(o.delivery_date - o.order_date)
        FROM orders o
        WHERE o.delivery_date IS NOT NULL
        AND o.order_date BETWEEN :startDate AND :endDate
        AND (CAST(:carriers AS text[]) IS NULL OR o.carrier = ANY(CAST(:carriers AS text[])))
        AND (CAST(:regions AS text[]) IS NULL OR o.region = ANY(CAST(:regions AS text[])))
        AND (CAST(:categories AS text[]) IS NULL OR o.product_category = ANY(CAST(:categories AS text[])))
        """, nativeQuery = true)
    Double averageDeliveryDaysByDateRangeWithFilters(@Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate,
                                                      @Param("carriers") String[] carriers,
                                                      @Param("regions") String[] regions,
                                                      @Param("categories") String[] categories);

    // ── Time series ───────────────────────────────────────────────────────────

    @Query(value = """
        SELECT period, COUNT(*) AS count
        FROM (
            SELECT
                CASE :granularity
                    WHEN 'day'   THEN o.order_date::date
                    WHEN 'week'  THEN date_trunc('week',  o.order_date)::date
                    WHEN 'month' THEN date_trunc('month', o.order_date)::date
                END AS period
            FROM orders o
            WHERE o.order_date BETWEEN :startDate AND :endDate
            AND (CAST(:carriers AS text[]) IS NULL OR o.carrier = ANY(CAST(:carriers AS text[])))
            AND (CAST(:regions AS text[]) IS NULL OR o.region = ANY(CAST(:regions AS text[])))
            AND (CAST(:categories AS text[]) IS NULL OR o.product_category = ANY(CAST(:categories AS text[])))
        ) sub
        GROUP BY period
        ORDER BY period
        """, nativeQuery = true)
    List<Object[]> getOrderCountByTimePeriodWithFilters(@Param("granularity") String granularity,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate,
                                                         @Param("carriers") String[] carriers,
                                                         @Param("regions") String[] regions,
                                                         @Param("categories") String[] categories);

    // ── Delivery performance (on-time vs delayed) ─────────────────────────────

    @Query(value = """
        SELECT period,
            SUM(CASE WHEN delivery_date IS NOT NULL AND delivery_date <= promised_delivery_date THEN 1 ELSE 0 END) AS on_time,
            SUM(CASE WHEN delivery_date > promised_delivery_date THEN 1 ELSE 0 END) AS delayed
        FROM (
            SELECT
                CASE :granularity
                    WHEN 'day'   THEN o.order_date::date
                    WHEN 'week'  THEN date_trunc('week',  o.order_date)::date
                    WHEN 'month' THEN date_trunc('month', o.order_date)::date
                END AS period,
                o.delivery_date,
                o.promised_delivery_date
            FROM orders o
            WHERE o.order_date BETWEEN :startDate AND :endDate
            AND (CAST(:carriers AS text[]) IS NULL OR o.carrier = ANY(CAST(:carriers AS text[])))
            AND (CAST(:regions AS text[]) IS NULL OR o.region = ANY(CAST(:regions AS text[])))
            AND (CAST(:categories AS text[]) IS NULL OR o.product_category = ANY(CAST(:categories AS text[])))
        ) sub
        GROUP BY period
        ORDER BY period
        """, nativeQuery = true)
    List<Object[]> getDeliveryPerformanceByTimePeriod(@Param("granularity") String granularity,
                                                       @Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate,
                                                       @Param("carriers") String[] carriers,
                                                       @Param("regions") String[] regions,
                                                       @Param("categories") String[] categories);

    // ── Carrier breakdown ─────────────────────────────────────────────────────

    @Query(value = """
        SELECT o.carrier,
               COUNT(*) AS total_orders,
               SUM(CASE WHEN o.delivery_date > o.promised_delivery_date THEN 1 ELSE 0 END) AS delayed_orders
        FROM orders o
        WHERE o.order_date BETWEEN :startDate AND :endDate
        AND (CAST(:carriers AS text[]) IS NULL OR o.carrier = ANY(CAST(:carriers AS text[])))
        AND (CAST(:regions AS text[]) IS NULL OR o.region = ANY(CAST(:regions AS text[])))
        AND (CAST(:categories AS text[]) IS NULL OR o.product_category = ANY(CAST(:categories AS text[])))
        GROUP BY o.carrier
        ORDER BY total_orders DESC
        """, nativeQuery = true)
    List<Object[]> getCarrierBreakdownWithFilters(@Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate,
                                                   @Param("carriers") String[] carriers,
                                                   @Param("regions") String[] regions,
                                                   @Param("categories") String[] categories);

    // ── Region breakdown ──────────────────────────────────────────────────────

    @Query(value = """
        SELECT o.region,
               COUNT(*) AS total_orders,
               SUM(CASE WHEN o.delivery_date > o.promised_delivery_date THEN 1 ELSE 0 END) AS delayed_orders
        FROM orders o
        WHERE o.order_date BETWEEN :startDate AND :endDate
        AND (CAST(:carriers AS text[]) IS NULL OR o.carrier = ANY(CAST(:carriers AS text[])))
        AND (CAST(:regions AS text[]) IS NULL OR o.region = ANY(CAST(:regions AS text[])))
        AND (CAST(:categories AS text[]) IS NULL OR o.product_category = ANY(CAST(:categories AS text[])))
        GROUP BY o.region
        ORDER BY total_orders DESC
        """, nativeQuery = true)
    List<Object[]> getRegionBreakdownWithFilters(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate,
                                                  @Param("carriers") String[] carriers,
                                                  @Param("regions") String[] regions,
                                                  @Param("categories") String[] categories);

    // ── Category breakdown ──────────────────────────────────────────────────

    @Query(value = """
        SELECT o.product_category,
               COUNT(*) AS total_orders,
               SUM(CASE WHEN o.delivery_date > o.promised_delivery_date THEN 1 ELSE 0 END) AS delayed_orders
        FROM orders o
        WHERE o.order_date BETWEEN :startDate AND :endDate
        AND (CAST(:carriers AS text[]) IS NULL OR o.carrier = ANY(CAST(:carriers AS text[])))
        AND (CAST(:regions AS text[]) IS NULL OR o.region = ANY(CAST(:regions AS text[])))
        AND (CAST(:categories AS text[]) IS NULL OR o.product_category = ANY(CAST(:categories AS text[])))
        GROUP BY o.product_category
        ORDER BY total_orders DESC
        """, nativeQuery = true)
    List<Object[]> getCategoryBreakdownWithFilters(@Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate,
                                                    @Param("carriers") String[] carriers,
                                                    @Param("regions") String[] regions,
                                                    @Param("categories") String[] categories);

    // ── Historical data for forecasting ──────────────────────────────────────

    @Query(value = """
        SELECT period, COUNT(*) AS count
        FROM (
            SELECT
                CASE :granularity
                    WHEN 'day'   THEN o.order_date::date
                    WHEN 'week'  THEN date_trunc('week',  o.order_date)::date
                    WHEN 'month' THEN date_trunc('month', o.order_date)::date
                END AS period
            FROM orders o
            WHERE o.order_date BETWEEN :startDate AND :endDate
            AND (CAST(:carriers AS text[]) IS NULL OR o.carrier = ANY(CAST(:carriers AS text[])))
            AND (CAST(:regions AS text[]) IS NULL OR o.region = ANY(CAST(:regions AS text[])))
            AND (CAST(:categories AS text[]) IS NULL OR o.product_category = ANY(CAST(:categories AS text[])))
        ) sub
        GROUP BY period
        ORDER BY period
        """, nativeQuery = true)
    List<Object[]> getHistoricalOrderCountByPeriod(@Param("granularity") String granularity,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate,
                                                    @Param("carriers") String[] carriers,
                                                    @Param("regions") String[] regions,
                                                    @Param("categories") String[] categories);

    /**
     * Get historical on-time rate or delay rate by time period.
     * metricType: 'on_time_rate' → (on-time / total delivered) * 100
     * metricType: 'delay_rate'   → (delayed / total) * 100
     */
    @Query(value = """
        SELECT period,
            CASE
                WHEN :metricType = 'on_time_rate' THEN
                    ROUND((SUM(CASE WHEN delivery_date IS NOT NULL AND delivery_date <= promised_delivery_date THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(*), 0)), 2)
                WHEN :metricType = 'delay_rate' THEN
                    ROUND((SUM(CASE WHEN delivery_date > promised_delivery_date THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(*), 0)), 2)
                ELSE 0
            END AS rate
        FROM (
            SELECT
                CASE :granularity
                    WHEN 'day'   THEN o.order_date::date
                    WHEN 'week'  THEN date_trunc('week',  o.order_date)::date
                    WHEN 'month' THEN date_trunc('month', o.order_date)::date
                END AS period,
                o.delivery_date,
                o.promised_delivery_date
            FROM orders o
            WHERE o.order_date BETWEEN :startDate AND :endDate
            AND o.delivery_date IS NOT NULL
            AND (CAST(:carriers AS text[]) IS NULL OR o.carrier = ANY(CAST(:carriers AS text[])))
            AND (CAST(:regions AS text[]) IS NULL OR o.region = ANY(CAST(:regions AS text[])))
            AND (CAST(:categories AS text[]) IS NULL OR o.product_category = ANY(CAST(:categories AS text[])))
        ) sub
        GROUP BY period
        ORDER BY period
        """, nativeQuery = true)
    List<Object[]> getHistoricalMetricByPeriod(@Param("metricType") String metricType,
                                                @Param("granularity") String granularity,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate,
                                                @Param("carriers") String[] carriers,
                                                @Param("regions") String[] regions,
                                                @Param("categories") String[] categories);
}
