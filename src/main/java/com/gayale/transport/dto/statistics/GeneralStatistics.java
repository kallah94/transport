package com.gayale.transport.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GeneralStatistics {

    private double totalTonnage;
    private int totalTrips;
    private int activeProjects;
    private int activePurchaseOrders;
    private double averageTonnagePerTrip;
    private double todayTonnage;
    private int todayTrips;
    private double weekTonnage;
    private int weekTrips;
    private double monthTonnage;
    private int monthTrips;
}