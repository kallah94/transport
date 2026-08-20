package com.gayale.transport.model;

import com.gayale.transport.util.AuditableEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Notification destinée à l'administration (cloche + polling). Multi-tenant via AuditableEntity
 * (tenantId posé automatiquement à l'écriture). Statut lu/non-lu conservé en base.
 */
@Document(collection = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Notification extends AuditableEntity {

    @Id
    private String id;

    private NotificationType type;
    private NotificationLevel level;
    private String title;
    private String message;

    /** Route front optionnelle vers l'objet concerné (ex: "/purchase-orders"). */
    private String link;
    /** Identifiant de l'entité liée (BC, transporteur, projet, paiement…). */
    private String refId;

    /** Cible camion (rôle DRIVER) : si renseigné, la notification est destinée à ce véhicule.
     *  Null = notification administration. */
    private String vehicle;

    @Builder.Default
    private boolean read = false;

    public enum NotificationType {
        PURCHASE_ORDER_THRESHOLD,
        PURCHASE_ORDER_COMPLETED,
        TRANSPORTER_CREATED,
        PROJECT_CREATED,
        PAYMENT_GENERATED,
        TRIP_RECORDED
    }

    public enum NotificationLevel {
        INFO,
        ALERT
    }
}
