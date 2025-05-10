package com.gayale.transport.model;

import com.gayale.transport.util.AuditableEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "purchase_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PurchaseOrder extends AuditableEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String orderNumber;

    private String supplier;

    private String transporter;

    private double orderedQuantity;

    private double deliveredQuantity;

    private double remainingQuantity;

    private double deliveryPercentage;

    private String projectId;

    private OrderStatus status;

    public enum OrderStatus {
        ACTIVE, COMPLETED, CANCELLED
    }

    // Method to recalculate delivery metrics
    public void recalculateDeliveryMetrics() {
        this.remainingQuantity = this.orderedQuantity - this.deliveredQuantity;
        this.deliveryPercentage = (this.deliveredQuantity / this.orderedQuantity) * 100;

        // Update status based on delivery percentage
        if (this.deliveryPercentage >= 100) {
            this.status = OrderStatus.COMPLETED;
        }
    }
}