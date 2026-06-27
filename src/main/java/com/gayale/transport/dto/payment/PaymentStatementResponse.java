package com.gayale.transport.dto.payment;

import com.gayale.transport.model.PaymentStatement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentStatementResponse {
    private String id;
    private String transporterId;
    private String transporterName;
    private String purchaseOrderId;
    private String purchaseOrderNumber;

    private String supplier;
    private String projectName;
    private String client;
    private String provenance;
    private String destination;
    private String product;
    private String transporterAddress;
    private String transporterRegistrationNumber;
    private String transporterPhone;
    private String transporterEmail;
    private String representativeName;
    private String representativeEmail;
    private double tonnageTotal;
    private int tripCount;
    private double unitPricePerTonne;
    private double transportAmount;
    private double fuelLitres;
    private double fuelAmount;
    private double invoiceTotal;
    private PaymentStatement.PaymentStatus status;
    private List<PaymentLineDto> lines;
    private LocalDateTime generatedAt;
}
