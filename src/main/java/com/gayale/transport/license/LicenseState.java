package com.gayale.transport.license;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

/**
 * Vue immuable de l'etat de licence a un instant donne : ce que le reste de l'application
 * consulte pour autoriser (ou non) une action. Recalculee a partir de la cle signee, jamais
 * a partir des seuls champs stockes en base.
 */
@Getter
@Builder
public class LicenseState {

    private final LicenseStatus status;

    private final LicensePlan plan;

    @Builder.Default
    private final Set<LicenseFeature> features = Collections.emptySet();

    private final String customer;
    private final String customerKey;
    private final String licenseId;

    private final LocalDate issuedAt;
    private final LocalDate expiresAt;

    /** Dernier jour d'utilisation tolere (expiration + periode de grace). */
    private final LocalDate graceUntil;

    /** Jours restants avant expiration ; negatif si deja expiree ; null si perpetuelle. */
    private final Integer daysRemaining;

    private final String machineId;

    @Builder.Default
    private final int maxUsers = 0;
    @Builder.Default
    private final int maxTrucks = 0;
    @Builder.Default
    private final int maxTicketsPerMonth = 0;

    @Builder.Default
    private final boolean trial = false;

    /** Vrai si l'enforcement est desactive par configuration (developpement). */
    @Builder.Default
    private final boolean enforcementDisabled = false;

    /** Message a afficher au client quand la licence bloque ou va bloquer. */
    private final String message;

    public boolean isValid() {
        return enforcementDisabled || (status != null && status.allowsUse());
    }

    public boolean has(LicenseFeature feature) {
        return enforcementDisabled || (features != null && features.contains(feature));
    }

    /** Etat renvoye quand l'enforcement est coupe : tout est permis, rien n'est limite. */
    public static LicenseState unrestricted() {
        return LicenseState.builder()
                .status(LicenseStatus.ACTIVE)
                .plan(LicensePlan.ENTERPRISE)
                .features(java.util.EnumSet.allOf(LicenseFeature.class))
                .customer("Enforcement desactive")
                .maxUsers(-1)
                .maxTrucks(-1)
                .maxTicketsPerMonth(-1)
                .enforcementDisabled(true)
                .build();
    }
}
