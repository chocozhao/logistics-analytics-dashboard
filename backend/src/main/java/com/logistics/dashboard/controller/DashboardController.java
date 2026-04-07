package com.logistics.dashboard.controller;

import com.logistics.dashboard.dto.*;
import com.logistics.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/kpis")
    public ResponseEntity<KpiResponse> getKPIs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<String> carrier,
            @RequestParam(required = false) List<String> region) {

        log.info("GET /api/dashboard/kpis - startDate={}, endDate={}, carrier={}, region={}",
                startDate, endDate, carrier, region);

        KpiResponse response = dashboardService.getKPIs(startDate, endDate, carrier, region);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order-volume")
    public ResponseEntity<TimeSeriesResponse> getOrderVolume(
            @RequestParam(defaultValue = "day") String granularity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<String> carrier,
            @RequestParam(required = false) List<String> region) {

        log.info("GET /api/dashboard/order-volume - granularity={}, startDate={}, endDate={}",
                granularity, startDate, endDate);

        if (!Arrays.asList("day", "week", "month").contains(granularity)) {
            return ResponseEntity.badRequest().build();
        }

        TimeSeriesResponse response = dashboardService.getOrderVolumeTimeSeries(
                granularity, startDate, endDate, carrier, region);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/delivery-performance")
    public ResponseEntity<DeliveryPerformanceResponse> getDeliveryPerformance(
            @RequestParam(defaultValue = "week") String granularity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<String> carrier,
            @RequestParam(required = false) List<String> region) {

        log.info("GET /api/dashboard/delivery-performance - granularity={}, startDate={}, endDate={}",
                granularity, startDate, endDate);

        if (!Arrays.asList("week", "month").contains(granularity)) {
            return ResponseEntity.badRequest().build();
        }

        DeliveryPerformanceResponse response = dashboardService.getDeliveryPerformance(
                granularity, startDate, endDate, carrier, region);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/carrier-breakdown")
    public ResponseEntity<CarrierBreakdownResponse> getCarrierBreakdown(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<String> carrier,
            @RequestParam(required = false) List<String> region) {

        log.info("GET /api/dashboard/carrier-breakdown - startDate={}, endDate={}", startDate, endDate);

        CarrierBreakdownResponse response = dashboardService.getCarrierBreakdown(
                startDate, endDate, carrier, region);
        return ResponseEntity.ok(response);
    }

    // Default date range endpoints
    @GetMapping("/kpis/recent")
    public ResponseEntity<KpiResponse> getRecentKPIs() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        return getKPIs(startDate, endDate, null, null);
    }

    @GetMapping("/order-volume/recent")
    public ResponseEntity<TimeSeriesResponse> getRecentOrderVolume(
            @RequestParam(defaultValue = "day") String granularity) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        return getOrderVolume(granularity, startDate, endDate, null, null);
    }
}