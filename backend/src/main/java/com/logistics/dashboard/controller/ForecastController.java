package com.logistics.dashboard.controller;

import com.logistics.dashboard.ai.model.ForecastRequest;
import com.logistics.dashboard.dto.ForecastResponse;
import com.logistics.dashboard.service.ForecastingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {

    private final ForecastingService forecastingService;

    public ForecastController(ForecastingService forecastingService) {
        this.forecastingService = forecastingService;
    }

    @PostMapping
    public ResponseEntity<ForecastResponse> forecast(@RequestBody ForecastRequest request) {
        if (request.getGranularity() == null || request.getPeriods() <= 0) {
            return ResponseEntity.badRequest().body(
                    new ForecastResponse(null, 0, "Invalid",
                            null, null, "Granularity and periods are required")
            );
        }

        // Use default date range if not provided
        // Database has data from 2024-01-01 to 2025-03-31
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : LocalDate.of(2025, 3, 31);
        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : endDate.minusMonths(6);

        ForecastResponse response = forecastingService.forecastDemand(
                request.getGranularity(),
                request.getPeriods(),
                startDate,
                endDate,
                request.getCarriers(),
                request.getRegions(),
                request.getCategories()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    public ResponseEntity<ForecastResponse> testForecast(
            @RequestParam(defaultValue = "week") String granularity,
            @RequestParam(defaultValue = "4") int periods,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<String> carriers,
            @RequestParam(required = false) List<String> regions) {

        LocalDate finalEndDate = endDate != null ? endDate : LocalDate.of(2025, 3, 31);
        LocalDate finalStartDate = startDate != null ? startDate : finalEndDate.minusMonths(3);

        ForecastResponse response = forecastingService.forecastDemand(
                granularity,
                periods,
                finalStartDate,
                finalEndDate,
                carriers,
                regions,
                null
        );

        return ResponseEntity.ok(response);
    }
}