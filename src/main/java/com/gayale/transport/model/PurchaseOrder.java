package com.gayale.transport.model;

import com.gayale.transport.util.AuditableEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "purchase_orders")
@CompoundIndex(name = "uk_tenant_order", def = "{'tenantId': 1, 'orderNumber': 1}", unique = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PurchaseOrder extends AuditableEntity {

    @Id
    private String id;

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
        // Garde-fou contre la division par zéro (orderedQuantity == 0 -> NaN)
        this.deliveryPercentage = this.orderedQuantity > 0
                ? (this.deliveredQuantity / this.orderedQuantity) * 100
                : 0;

        // Update status based on delivery percentage
        if (this.deliveryPercentage >= 100) {
            this.status = OrderStatus.COMPLETED;
        }
    }
}