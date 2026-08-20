package com.gayale.transport.dto.driver;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs de l'application mobile chauffeur (données filtrées par camion `vehicle`).
 */
public final class DriverDto {

    private DriverDto() {
    }

    /** Profil du camion + statistiques rapides. */
    public record Profile(
            String vehicle,
            String transporterName,
            double capacityTonnes,
            double fuelConsumptionLPerKm,
            double tonnesThisMonth,
            long tripsThisMonth,
            double totalTonnes,
            long totalTrips
    ) {}

    /** Un trajet (ticket de pesée) du camion, avec le détail des pesées. */
    public record Trip(
            String id,
            String ticketNumber,
            LocalDate date,
            String vehicle,
            String driver,
            String product,
            String projectId,
            String projectName,
            String client,
            String supplier,
            String purchaseOrderNumber,
            String origin,
            String destination,
            String operatorName,
            double emptyWeight,
            double loadedWeight,
            double netWeight,
            double tonnes,
            String status,
            LocalDateTime createdAt
    ) {}

    /** Un projet où le camion est intervenu + progression + part du camion. */
    public record Project(
            String projectId,
            String name,
            String client,
            String destination,
            String provenance,
            String product,
            String status,
            double projectDeliveredTonnage,
            double myTonnes,
            long myTrips
    ) {}

    /** Agrégat générique (par projet ou par mois). */
    public record StatBucket(
            String key,
            String label,
            double tonnes,
            long trips
    ) {}

    /** Statistiques du camion. */
    public record Stats(
            double totalTonnes,
            long totalTrips,
            List<StatBucket> byProject,
            List<StatBucket> byMonth
    ) {}
}
