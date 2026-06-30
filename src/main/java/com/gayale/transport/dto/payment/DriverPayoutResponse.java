package com.gayale.transport.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverPayoutResponse {
    private String id;
    private String status;
    private String transporterId;
    private String transporterName;
    private String purchaseOrderId;
    private String purchaseOrderNumber;

    private List<DriverPayoutLineDto> lines;

    // Totaux
    private double totalTonnes;
    private double totalFuelLitres;
    private double totalFuelAmount;
    private double totalGross;
    private double totalNet;
}
