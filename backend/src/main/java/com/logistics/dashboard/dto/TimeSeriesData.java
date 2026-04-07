package com.logistics.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesData {
    private LocalDate date;
    private Long count;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class TimeSeriesResponse {
    private String granularity; // "day", "week", "month"
    private java.util.List<TimeSeriesData> data;
}