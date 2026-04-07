package com.logistics.dashboard.dto;

import java.math.BigDecimal;

public class CarrierBreakdown {
    private String carrier;
    private Long totalOrders;
    private Long delayedOrders;
    private BigDecimal delayRate; // percentage

    // Constructors
    public CarrierBreakdown() {}

    public CarrierBreakdown(String carrier, Long totalOrders, Long delayedOrders, BigDecimal delayRate) {
        this.carrier = carrier;
        this.totalOrders = totalOrders;
        this.delayedOrders = delayedOrders;
        this.delayRate = delayRate;
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
}

