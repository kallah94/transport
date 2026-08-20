package com.gayale.transport.license;

import lombok.Getter;

/**
 * Levee quand une operation est refusee par la licence : plan insuffisant, quota atteint,
 * licence absente ou expiree. Traduite en HTTP 402 (Payment Required) par le
 * GlobalExceptionHandler, code que le frontend intercepte pour rediriger vers /license.
 */
@Getter
public class LicenseRequiredException extends RuntimeException {

    /** Etat de la licence au moment du refus. */
    private final LicenseStatus status;

    /** Fonctionnalite manquante, si le refus vient d'un plan insuffisant. */
    private final LicenseFeature feature;

    /** Quota depasse, si le refus vient d'une limite (ex: "users", "trucks", "ticketsPerMonth"). */
    private final String quota;

    public LicenseRequiredException(LicenseStatus status, String message) {
        this(status, message, null, null);
    }

    public LicenseRequiredException(LicenseStatus status, String message, LicenseFeature feature, String quota) {
        super(message);
        this.status = status;
        this.feature = feature;
        this.quota = quota;
    }

    public static LicenseRequiredException feature(LicenseFeature feature) {
        return new LicenseRequiredException(
                LicenseStatus.ACTIVE,
                "Le module « " + feature.name() + " » n'est pas inclus dans votre licence.",
                feature, null);
    }

    public static LicenseRequiredException quota(String quota, long current, int max) {
        return new LicenseRequiredException(
                LicenseStatus.ACTIVE,
                "Quota de licence atteint (" + quota + ") : " + current + " / " + max + ".",
                null, quota);
    }
}
