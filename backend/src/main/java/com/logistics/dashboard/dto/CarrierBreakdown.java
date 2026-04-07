package com.logistics.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarrierBreakdown {
    private String carrier;
    private Long totalOrders;
    private Long delayedOrders;
    private BigDecimal delayRate; // percentage
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class CarrierBreakdownResponse {
    private java.util.List<CarrierBreakdown> data;
}