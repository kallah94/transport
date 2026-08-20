package com.gayale.transport.license;

/**
 * Fonctionnalites vendables. Chaque plan porte un sous-ensemble de ces drapeaux ;
 * la cle de licence peut en ajouter (add-on negocie) via le champ "ft".
 *
 * IMPORTANT : ne JAMAIS renommer ni reordonner une constante existante — les cles de
 * licence deja emises chez les clients contiennent ces noms en clair dans leur payload.
 * Pour retirer une fonctionnalite, marquez-la @Deprecated et laissez-la en place.
 */
public enum LicenseFeature {

    /** Saisie / suivi des tickets de pesee. Socle : present dans tous les plans. */
    WEIGHT_TICKETS,

    /** Gestion des projets et bons de commande. Socle. */
    PROJECTS,

    /** Gestion des camions et transporteurs. Socle. */
    FLEET,

    /** Module Facturation transporteur (releves, validation, paiement). */
    PAYMENTS,

    /** Module Paiement chauffeur (payouts, releves chauffeur). */
    DRIVER_PAYOUTS,

    /** Grille tarifaire + dotation carburant. */
    PRICING_FUEL,

    /** Statistiques avancees et tableaux de bord analytiques. */
    ADVANCED_STATISTICS,

    /** Exports PDF / Excel des rapports. */
    EXPORTS,

    /** Application mobile chauffeur (comptes de role DRIVER + endpoints /api/driver). */
    DRIVER_MOBILE_APP,

    /** Notifications applicatives. */
    NOTIFICATIONS,

    /** Personnalisation de marque par le client (theme, logo). */
    WHITE_LABEL,

    /** Acces multi-tenant / multi-sites sur une meme instance. */
    MULTI_TENANT
}
