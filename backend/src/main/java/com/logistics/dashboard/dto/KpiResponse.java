package com.logistics.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KpiResponse {
    private Long totalOrders;
    private Long deliveredOrders;
    private Long delayedOrders;
    private BigDecimal onTimeRate; // percentage
    private BigDecimal avgDeliveryDays;
}