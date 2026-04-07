package com.logistics.dashboard.dto;

public class DeliveryPerformance {
    private String period; // e.g., "2024-W01", "2024-01", depending on granularity
    private Long onTime;
    private Long delayed;

    // Constructors
    public DeliveryPerformance() {}

    public DeliveryPerformance(String period, Long onTime, Long delayed) {
        this.period = period;
        this.onTime = onTime;
        this.delayed = delayed;
    }

    // Getters and setters
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public Long getOnTime() { return onTime; }
    public void setOnTime(Long onTime) { this.onTime = onTime; }

    public Long getDelayed() { return delayed; }
    public void setDelayed(Long delayed) { this.delayed = delayed; }
}

