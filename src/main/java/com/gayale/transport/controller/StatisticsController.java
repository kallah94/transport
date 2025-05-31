package com.gayale.transport.controller;

import com.gayale.transport.dto.statistics.GeneralStatistics;
import com.gayale.transport.dto.statistics.StatisticsDate;
import com.gayale.transport.dto.statistics.StatisticsEntity;
import com.gayale.transport.dto.statistics.StatisticsPeriod;
import com.gayale.transport.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/statistics")
@SecurityRequirement(name = "JWT")
@Tag(name = "Statistics", description = "API for statistics and reporting")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Autowired
    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/general")
    @Operation(summary = "Get general statistics",
            description = "Returns general statistics including totals, active projects/orders, and period breakdowns")
    public ResponseEntity<GeneralStatistics> getGeneralStatistics() {
        GeneralStatistics statistics = statisticsService.getGeneralStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/period")
    @Operation(summary = "Get statistics for a specific period",
            description = "Returns statistics for a date range between start date and end date")
    public ResponseEntity<StatisticsPeriod> getStatisticsByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        StatisticsPeriod statistics = statisticsService.getStatisticsByPeriod(startDate, endDate);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/date")
    @Operation(summary = "Get statistics for a specific date",
    description = "Returns general statistics for a given date")
    public ResponseEntity<StatisticsDate> getStatisticsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        StatisticsDate statistics = statisticsService.getStatisticsByDate(date);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/range-date")
    @Operation(summary = "Get statistics for a range of date",
    description = "Returns general statistics entry for each date in the range")
    public ResponseEntity<List<StatisticsDate>> getStatisticsByDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<StatisticsDate> statistics = statisticsService.getStatisticsByDates(startDate, endDate);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/project")
    @Operation(summary = "Get statistics by project",
            description = "Returns statistics grouped by projects")
    public ResponseEntity<List<StatisticsEntity>> getStatisticsByProject() {
        List<StatisticsEntity> statistics = statisticsService.getStatisticsByProject();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/purchase-order")
    @Operation(summary = "Get statistics by purchase order",
            description = "Returns statistics grouped by purchase orders")
    public ResponseEntity<List<StatisticsEntity>> getStatisticsByPurchaseOrder() {
        List<StatisticsEntity> statistics = statisticsService.getStatisticsByPurchaseOrder();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/transporter")
    @Operation(summary = "Get statistics by transporter",
            description = "Returns statistics grouped by transporters")
    public ResponseEntity<List<StatisticsEntity>> getStatisticsByTransporter() {
        List<StatisticsEntity> statistics = statisticsService.getStatisticsByTransporter();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/vehicle")
    @Operation(summary = "Get statistics by vehicle",
            description = "Returns statistics grouped by vehicles")
    public ResponseEntity<List<StatisticsEntity>> getStatisticsByVehicle() {
        List<StatisticsEntity> statistics = statisticsService.getStatisticsByVehicle();
        return ResponseEntity.ok(statistics);
    }
}