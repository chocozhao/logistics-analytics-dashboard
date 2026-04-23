package com.logistics.dashboard.dto;

import java.math.BigDecimal;

public class RegionBreakdown {
    private String region;
    private Long totalOrders;
    private Long delayedOrders;
    private BigDecimal delayRate; // percentage
    private BigDecimal onTimeRate; // percentage

    public RegionBreakdown() {}

    public RegionBreakdown(String region, Long totalOrders, Long delayedOrders, BigDecimal delayRate) {
        this(region, totalOrders, delayedOrders, delayRate,
             delayRate != null ? BigDecimal.valueOf(100).subtract(delayRate) : BigDecimal.ZERO);
    }

    public RegionBreakdown(String region, Long totalOrders, Long delayedOrders,
                           BigDecimal delayRate, BigDecimal onTimeRate) {
        this.region = region;
        this.totalOrders = totalOrders;
        this.delayedOrders = delayedOrders;
        this.delayRate = delayRate;
        this.onTimeRate = onTimeRate;
    }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }

    public Long getDelayedOrders() { return delayedOrders; }
    public void setDelayedOrders(Long delayedOrders) { this.delayedOrders = delayedOrders; }

    public BigDecimal getDelayRate() { return delayRate; }
    public void setDelayRate(BigDecimal delayRate) { this.delayRate = delayRate; }

    public BigDecimal getOnTimeRate() { return onTimeRate; }
    public void setOnTimeRate(BigDecimal onTimeRate) { this.onTimeRate = onTimeRate; }
}
