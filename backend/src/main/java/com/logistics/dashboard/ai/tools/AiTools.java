package com.logistics.dashboard.ai.tools;

import com.logistics.dashboard.dto.*;
import com.logistics.dashboard.service.DashboardService;
import com.logistics.dashboard.service.ForecastingService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class AiTools {

    private final DashboardService dashboardService;
    private final ForecastingService forecastingService;

    public AiTools(DashboardService dashboardService, ForecastingService forecastingService) {
        this.dashboardService = dashboardService;
        this.forecastingService = forecastingService;
    }

    /**
     * Get time series data for order volume or other metrics over time
     */
    @Tool("Get aggregated values over time with specified granularity (day, week, month). " +
          "Use this when the user asks about trends, changes over time, or historical patterns. " +
          "Examples: 'Show me orders per day last month', 'How did order volume change week by week?'")
    public TimeSeriesResponse getTimeSeries(
            @P("The time granularity: 'day', 'week', or 'month'") String granularity,
            @P("Start date in YYYY-MM-DD format") LocalDate startDate,
            @P("End date in YYYY-MM-DD format") LocalDate endDate,
            @P("Optional list of carriers to filter by") List<String> carriers,
            @P("Optional list of regions to filter by") List<String> regions) {

        return dashboardService.getOrderVolumeTimeSeries(granularity, startDate, endDate, carriers, regions);
    }

    /**
     * Get breakdown of data by categorical dimension (carrier, region, etc.)
     */
    @Tool("Get aggregated values by categorical dimension like carrier, region, or status. " +
          "Use this when the user asks for comparison, breakdown, or distribution. " +
          "Examples: 'Compare carriers by delay rate', 'Show orders by region', 'Which carrier has most delays?'")
    public CarrierBreakdownResponse getBreakdown(
            @P("The dimension to breakdown by: 'carrier' or 'region'") String dimension,
            @P("Start date in YYYY-MM-DD format") LocalDate startDate,
            @P("End date in YYYY-MM-DD format") LocalDate endDate,
            @P("Optional list of carriers to filter by") List<String> carriers,
            @P("Optional list of regions to filter by") List<String> regions) {

        // For now, we only support carrier breakdown
        if ("carrier".equalsIgnoreCase(dimension)) {
            return dashboardService.getCarrierBreakdown(startDate, endDate, carriers, regions);
        } else {
            // For region breakdown, we'd need to implement a new method
            // For now, return carrier breakdown as fallback
            return dashboardService.getCarrierBreakdown(startDate, endDate, carriers, regions);
        }
    }

    /**
     * Get key performance indicators (KPIs)
     */
    @Tool("Get key performance indicators like total orders, delivered orders, delayed orders, on-time rate, average delivery days. " +
          "Use this when the user asks for summary statistics, performance metrics, or overall numbers. " +
          "Examples: 'How many orders last week?', 'What was the on-time delivery rate?', 'Show me the KPIs for Q1'")
    public KpiResponse getKpi(
            @P("Start date in YYYY-MM-DD format") LocalDate startDate,
            @P("End date in YYYY-MM-DD format") LocalDate endDate,
            @P("Optional list of carriers to filter by") List<String> carriers,
            @P("Optional list of regions to filter by") List<String> regions) {

        return dashboardService.getKPIs(startDate, endDate, carriers, regions);
    }

    /**
     * Get delivery performance data (on-time vs delayed)
     */
    @Tool("Get delivery performance data showing on-time vs delayed breakdown over time. " +
          "Use this when the user asks about delivery performance, on-time rates over time, or delay trends. " +
          "Examples: 'Show delivery performance by week', 'How did on-time rate change month by month?'")
    public DeliveryPerformanceResponse getDeliveryPerformance(
            @P("The time granularity: 'day', 'week', or 'month'") String granularity,
            @P("Start date in YYYY-MM-DD format") LocalDate startDate,
            @P("End date in YYYY-MM-DD format") LocalDate endDate,
            @P("Optional list of carriers to filter by") List<String> carriers,
            @P("Optional list of regions to filter by") List<String> regions) {

        return dashboardService.getDeliveryPerformance(granularity, startDate, endDate, carriers, regions);
    }

    /**
     * Forecast future demand
     */
    @Tool("Forecast future order volume based on historical data. " +
          "Use this when the user asks for predictions, forecasts, or future trends. " +
          "Examples: 'Predict orders for next month', 'Forecast demand for Q3', 'What will order volume be next week?'")
    public ForecastResponse forecastDemand(
            @P("Number of periods to forecast") int periods,
            @P("The time granularity: 'day', 'week', or 'month'") String granularity,
            @P("Start date in YYYY-MM-DD format") LocalDate startDate,
            @P("End date in YYYY-MM-DD format") LocalDate endDate,
            @P("Optional list of carriers to filter by") List<String> carriers,
            @P("Optional list of regions to filter by") List<String> regions) {

        return forecastingService.forecastDemand(granularity, periods, startDate, endDate, carriers, regions);
    }
}