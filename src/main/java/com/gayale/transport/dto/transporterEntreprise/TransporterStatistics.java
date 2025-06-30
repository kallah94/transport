package com.gayale.transport.dto.transporterEntreprise;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
public class TransporterStatistics {
    private final long totalTransporters;
    private final long activeTransporters;
    private final int totalTrucks;
    private final int recentlyCreated;
}
