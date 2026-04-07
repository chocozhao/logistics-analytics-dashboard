package com.logistics.dashboard.dto;

import java.util.List;

public class DeliveryPerformanceResponse {
    private String granularity; // "week", "month"
    private List<DeliveryPerformance> data;

    // Constructors
    public DeliveryPerformanceResponse() {}

    public DeliveryPerformanceResponse(String granularity, List<DeliveryPerformance> data) {
        this.granularity = granularity;
        this.data = data;
    }

    // Getters and setters
    public String getGranularity() { return granularity; }
    public void setGranularity(String granularity) { this.granularity = granularity; }

    public List<DeliveryPerformance> getData() { return data; }
    public void setData(List<DeliveryPerformance> data) { this.data = data; }
}