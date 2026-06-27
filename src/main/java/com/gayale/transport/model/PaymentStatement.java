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

@Document(collection = "payment_statements")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentStatement extends AuditableEntity {

    @Id
    private String id;

    @Indexed
    private String transporterId;
    private String transporterName;

    private String purchaseOrderId;
    private String purchaseOrderNumber;

    // Infos en-tete de facture (denormalisees)
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

    // --- Facture entreprise -> transporteur ---
    private double unitPricePerTonne; // prix unitaire facture (XOF/tonne)
    private double transportAmount;   // transport facture a la tonne
    private double fuelLitres;
    private double fuelAmount;         // dotation carburant
    private double invoiceTotal;       // transportAmount + fuelAmount (ce que l'entreprise paie)

    private PaymentStatus status;

    private List<PaymentLine> lines;

    private LocalDateTime generatedAt;

    public enum PaymentStatus {
        DRAFT, VALIDATED, PAID
    }
}
