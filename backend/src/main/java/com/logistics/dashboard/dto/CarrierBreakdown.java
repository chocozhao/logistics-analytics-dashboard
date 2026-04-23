package com.logistics.dashboard.dto;

import java.math.BigDecimal;

public class CarrierBreakdown {
    private String carrier;
    private Long totalOrders;
    private Long delayedOrders;
    private BigDecimal delayRate; // percentage
    private BigDecimal onTimeRate; // percentage

    // Constructors
    public CarrierBreakdown() {}

    public CarrierBreakdown(String carrier, Long totalOrders, Long delayedOrders, BigDecimal delayRate) {
        this(carrier, totalOrders, delayedOrders, delayRate,
             delayRate != null ? BigDecimal.valueOf(100).subtract(delayRate) : BigDecimal.ZERO);
    }

    public CarrierBreakdown(String carrier, Long totalOrders, Long delayedOrders,
                            BigDecimal delayRate, BigDecimal onTimeRate) {
        this.carrier = carrier;
        this.totalOrders = totalOrders;
        this.delayedOrders = delayedOrders;
        this.delayRate = delayRate;
        this.onTimeRate = onTimeRate;
    }

    // Getters and setters
    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) { this.carrier = carrier; }

    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }

    public Long getDelayedOrders() { return delayedOrders; }
    public void setDelayedOrders(Long delayedOrders) { this.delayedOrders = delayedOrders; }

    public BigDecimal getDelayRate() { return delayRate; }
    public void setDelayRate(BigDecimal delayRate) { this.delayRate = delayRate; }

    public BigDecimal getOnTimeRate() { return onTimeRate; }
    public void setOnTimeRate(BigDecimal onTimeRate) { this.onTimeRate = onTimeRate; }
}

