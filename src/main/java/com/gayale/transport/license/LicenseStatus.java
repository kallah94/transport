package com.gayale.transport.license;

/**
 * Etat resolu de la licence de l'installation courante.
 * Seuls ACTIVE et GRACE autorisent l'usage normal du produit.
 */
public enum LicenseStatus {

    /** Licence valide et non expiree. */
    ACTIVE,

    /** Licence expiree mais dans la periode de grace : usage autorise, bandeau d'alerte. */
    GRACE,

    /** Aucune licence installee (et pas d'essai en cours). */
    MISSING,

    /** Periode d'essai automatique en cours. */
    TRIAL,

    /** Licence expiree, periode de grace epuisee. */
    EXPIRED,

    /** Signature invalide, format corrompu ou cle emise pour un autre produit. */
    INVALID,

    /** Licence liee a une autre machine que celle-ci. */
    MACHINE_MISMATCH,

    /** Licence revoquee manuellement par l'editeur (liste de revocation). */
    REVOKED;

    /** Vrai si cet etat autorise l'utilisation des fonctionnalites du produit. */
    public boolean allowsUse() {
        return this == ACTIVE || this == GRACE || this == TRIAL;
    }
}
