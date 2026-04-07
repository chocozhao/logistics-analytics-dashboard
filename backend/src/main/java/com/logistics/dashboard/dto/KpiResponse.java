package com.logistics.dashboard.dto;

import java.math.BigDecimal;

public class KpiResponse {
    private Long totalOrders;
    private Long deliveredOrders;
    private Long delayedOrders;
    private BigDecimal onTimeRate; // percentage
    private BigDecimal avgDeliveryDays;

    // Constructors
    public KpiResponse() {}

    public KpiResponse(Long totalOrders, Long deliveredOrders, Long delayedOrders,
                      BigDecimal onTimeRate, BigDecimal avgDeliveryDays) {
        this.totalOrders = totalOrders;
        this.deliveredOrders = deliveredOrders;
        this.delayedOrders = delayedOrders;
        this.onTimeRate = onTimeRate;
        this.avgDeliveryDays = avgDeliveryDays;
    }

    // Getters and setters
    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }

    public Long getDeliveredOrders() { return deliveredOrders; }
    public void setDeliveredOrders(Long deliveredOrders) { this.deliveredOrders = deliveredOrders; }

    public Long getDelayedOrders() { return delayedOrders; }
    public void setDelayedOrders(Long delayedOrders) { this.delayedOrders = delayedOrders; }

    public BigDecimal getOnTimeRate() { return onTimeRate; }
    public void setOnTimeRate(BigDecimal onTimeRate) { this.onTimeRate = onTimeRate; }

    public BigDecimal getAvgDeliveryDays() { return avgDeliveryDays; }
    public void setAvgDeliveryDays(BigDecimal avgDeliveryDays) { this.avgDeliveryDays = avgDeliveryDays; }
}