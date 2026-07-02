package com.gayale.transport.model;

import com.gayale.transport.util.AuditableEntity;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;


@Document(collection = "trucks")
@CompoundIndex(name = "uk_tenant_vehicle", def = "{'tenantId': 1, 'vehicle': 1}", unique = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Truck extends AuditableEntity {

    @Id
    private String id;

    private String vehicle;

    @DBRef
    @NotNull(message = "Transporter is required")
    private TransporterEnterprise transporter;

    private String phone;

    private String driverName;

    // Capacite utile en tonnes (informatif)
    private double capacityTonnes;

    // Dotation carburant : consommation en L/km (saisie directe par camion)
    private double fuelConsumptionLPerKm;

}
