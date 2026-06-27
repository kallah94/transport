package com.gayale.transport.model;

import com.gayale.transport.util.AuditableEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "fuel_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FuelConfig extends AuditableEntity {

    @Id
    private String id;

    // Prix du carburant en XOF / litre
    private double fuelPricePerLitre;

    // Facteur aller-retour (defaut 2.0 : le camion revient a vide)
    private double roundTripFactor;

    // Mode de prise en compte de la dotation carburant
    private DotationMode dotationMode;

    private LocalDate effectiveFrom;

    private boolean active = true;

    public enum DotationMode {
        INCLUS,   // ajoutee au paiement transporteur
        SEPARE,   // affichee a part (avance carburant)
        DEDUIT    // retranchee du paiement (carburant avance par la societe)
    }
}
