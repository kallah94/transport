package com.gayale.transport.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsEntity {

    public enum EntityType {
        PROJECT, PURCHASE_ORDER, TRANSPORTER, VEHICLE
    }

    private EntityType entityType;
    private String entityId;
    private String entityName;
    private double totalTonnage;
    private int totalTrips;
    private double averageTonnagePerTrip;
}
