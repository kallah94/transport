package com.gayale.transport.model;

import com.gayale.transport.util.AuditableEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "driver_rates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DriverRate extends AuditableEntity {

    @Id
    private String id;

    // Tarif chauffeur en XOF / tonne (le XOF/trajet est derive par ticket)
    private double pricePerTonne;

    // Optionnel : bareme propre a un transporteur
    private String transporterId;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private boolean active = true;
}
