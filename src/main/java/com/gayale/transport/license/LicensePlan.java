package com.gayale.transport.license;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Catalogue commercial. Un plan definit les quotas par defaut et le socle de fonctionnalites.
 * La cle de licence peut surcharger les quotas (champs "lu"/"lt"/"lm") et ajouter des
 * fonctionnalites (champ "ft") : c'est ainsi qu'on vend un add-on sans creer un nouveau plan.
 *
 * Quota = -1 signifie "illimite".
 */
public enum LicensePlan {

    /**
     * Essai automatique a la premiere installation. Non vendu : genere localement,
     * limite dans le temps par {@code app.license.trial-days}.
     */
    TRIAL(
            "Essai",
            3, 5, 300,
            EnumSet.of(LicenseFeature.WEIGHT_TICKETS, LicenseFeature.PROJECTS, LicenseFeature.FLEET,
                    LicenseFeature.EXPORTS, LicenseFeature.NOTIFICATIONS)),

    /** Petit transporteur : saisie des tickets, projets, flotte, exports. */
    STARTER(
            "Starter",
            5, 10, 2_000,
            EnumSet.of(LicenseFeature.WEIGHT_TICKETS, LicenseFeature.PROJECTS, LicenseFeature.FLEET,
                    LicenseFeature.EXPORTS, LicenseFeature.NOTIFICATIONS)),

    /** Transporteur etabli : ajoute facturation, tarifs/carburant, payouts chauffeur, stats. */
    PRO(
            "Pro",
            25, 60, 25_000,
            EnumSet.of(LicenseFeature.WEIGHT_TICKETS, LicenseFeature.PROJECTS, LicenseFeature.FLEET,
                    LicenseFeature.EXPORTS, LicenseFeature.NOTIFICATIONS,
                    LicenseFeature.PAYMENTS, LicenseFeature.PRICING_FUEL,
                    LicenseFeature.DRIVER_PAYOUTS, LicenseFeature.ADVANCED_STATISTICS)),

    /** Groupe / multi-sites : tout, sans quota, marque blanche et app chauffeur incluses. */
    ENTERPRISE(
            "Enterprise",
            -1, -1, -1,
            EnumSet.allOf(LicenseFeature.class));

    private final String label;
    private final int maxUsers;
    private final int maxTrucks;
    private final int maxTicketsPerMonth;
    private final Set<LicenseFeature> features;

    LicensePlan(String label, int maxUsers, int maxTrucks, int maxTicketsPerMonth, Set<LicenseFeature> features) {
        this.label = label;
        this.maxUsers = maxUsers;
        this.maxTrucks = maxTrucks;
        this.maxTicketsPerMonth = maxTicketsPerMonth;
        this.features = Collections.unmodifiableSet(features);
    }

    public String getLabel() {
        return label;
    }

    public int getMaxUsers() {
        return maxUsers;
    }

    public int getMaxTrucks() {
        return maxTrucks;
    }

    public int getMaxTicketsPerMonth() {
        return maxTicketsPerMonth;
    }

    public Set<LicenseFeature> getFeatures() {
        return features;
    }

    /** Tolerant : cle inconnue ou nulle -> vide (la licence sera jugee INVALID). */
    public static LicensePlan from(String raw) {
        if (raw == null) {
            return null;
        }
        for (LicensePlan p : values()) {
            if (p.name().equalsIgnoreCase(raw.trim())) {
                return p;
            }
        }
        return null;
    }
}
