package com.logistics.dashboard.dto;

import java.util.List;

public class TimeSeriesResponse {
    private String granularity; // "day", "week", "month"
    private List<TimeSeriesData> data;

    // Constructors
    public TimeSeriesResponse() {}

    public TimeSeriesResponse(String granularity, List<TimeSeriesData> data) {
        this.granularity = granularity;
        this.data = data;
    }

    // Getters and setters
    public String getGranularity() { return granularity; }
    public void setGranularity(String granularity) { this.granularity = granularity; }

    public List<TimeSeriesData> getData() { return data; }
    public void setData(List<TimeSeriesData> data) { this.data = data; }
}