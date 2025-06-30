package com.gayale.transport.dto.statistics;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsDate {
    private LocalDate date;
    private double totalTonnage;
    private int totalTrips;
    private double averageTonnagePerTrip;
}
