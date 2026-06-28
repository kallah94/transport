package com.gayale.transport.dto.truck;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TruckStatistics {

    private String id;
    private String vehicle;
    private String driverName;
    private String transporterName;
    private double capacityTonnes;
    private double fuelConsumptionLPerKm;

    private int totalTrips;
    private double totalTonnage;
    private double averageTonnagePerTrip;
    private double capacityUtilizationPct;
    private LocalDate firstTripDate;
    private LocalDate lastTripDate;

    // Remuneration du camion (chauffeur) sur l'ensemble de son activite
    private double grossToReceive;     // tonnage x tarif chauffeur
    private double totalFuelLitres;    // dotation carburant (litres)
    private double totalFuelAmount;    // dotation carburant (XOF)
    private double netToReceive;       // total a percevoir = gross - carburant

    private List<Breakdown> byProject;
    private List<Breakdown> byMonth;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Breakdown {
        private String label;
        private int trips;
        private double tonnage;
    }
}
