package com.gayale.transport.model;

import com.gayale.transport.util.AuditableEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "weight_tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WeightTicket extends AuditableEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String ticketNumber;

    private LocalDate date;

    private double emptyWeight;

    private double loadedWeight;

    private double netWeight;

    private double variance;

    private String vehicle;

    private String driver;

    private String product;

    private String client;

    private String supplier;

    private String origin;

    private String destination;

    private String transporter;

    private String purchaseOrderNumber;

    private String projectId;

    private String purchaseOrderId;

    private String operatorName;

    private String qrCode;

    private TicketStatus status;

    public enum TicketStatus {
        PENDING, VALIDATED, CANCELLED
    }

    // Calculate net weight and variance
    public void calculateWeights() {
        this.netWeight = this.loadedWeight - this.emptyWeight;
        // Variance calculation could be implemented based on specific business rules
        // For example, variance could be the difference between expected and actual weight
        this.variance = 0.0; // Placeholder for custom variance calculation
    }
}
