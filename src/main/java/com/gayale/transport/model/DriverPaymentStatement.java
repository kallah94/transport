package com.gayale.transport.model;

import com.gayale.transport.util.AuditableEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "driver_payment_statements")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DriverPaymentStatement extends AuditableEntity {

    @Id
    private String id;

    @Indexed
    private String transporterId;
    private String transporterName;
    private String purchaseOrderId;
    private String purchaseOrderNumber;

    private List<DriverPayoutLine> lines;

    private double totalTonnes;
    private double totalFuelLitres;
    private double totalFuelAmount;
    private double totalGross;
    private double totalNet;

    private PaymentStatus status;
    private LocalDateTime generatedAt;

    public enum PaymentStatus { DRAFT, VALIDATED, PAID }
}
