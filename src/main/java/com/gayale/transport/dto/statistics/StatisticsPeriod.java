package com.gayale.transport.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsPeriod {

    private LocalDate startDate;
    private LocalDate endDate;
    private double totalTonnage;
    private int totalTrips;
    private double averageTonnagePerTrip;
}

