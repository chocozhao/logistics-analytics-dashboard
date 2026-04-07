package com.logistics.dashboard.dto;

import java.time.LocalDate;

public class TimeSeriesData {
    private LocalDate date;
    private Long count;

    // Constructors
    public TimeSeriesData() {}

    public TimeSeriesData(LocalDate date, Long count) {
        this.date = date;
        this.count = count;
    }

    // Getters and setters
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
}

