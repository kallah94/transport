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

@Document(collection = "transporter_rates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TransporterRate extends AuditableEntity {

    @Id
    private String id;

    @Indexed
    private String transporterId;

    // Tarif en XOF / tonne
    private double pricePerTonne;

    // Optionnel : tarif specifique a un projet
    private String projectId;

    // Optionnel : tarif specifique a un produit
    private String product;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private boolean active = true;
}
