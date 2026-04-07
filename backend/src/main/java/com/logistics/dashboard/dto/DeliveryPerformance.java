package com.logistics.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPerformance {
    private String period; // e.g., "2024-W01", "2024-01", depending on granularity
    private Long onTime;
    private Long delayed;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class DeliveryPerformanceResponse {
    private String granularity; // "week", "month"
    private java.util.List<DeliveryPerformance> data;
}