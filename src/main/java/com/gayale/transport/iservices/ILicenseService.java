package com.gayale.transport.iservices;

import com.gayale.transport.license.LicenseFeature;
import com.gayale.transport.license.LicenseState;
import com.gayale.transport.model.License;

/**
 * Cycle de vie de la licence de l'installation : resolution, activation, retrait,
 * et point d'entree des controles d'autorisation commerciale.
 */
public interface ILicenseService {

    /** Etat courant (verifie cryptographiquement, mis en cache quelques secondes). */
    LicenseState current();

    /** Active une cle de licence saisie par un administrateur. */
    License activate(String rawKey, String activatedBy);

    /** Desactive la licence courante (retour a l'etat MISSING / essai epuise). */
    void deactivate();

    /** Empreinte de la machine courante, a communiquer a l'editeur pour emettre une cle liee. */
    String machineId();

    /** Leve LicenseRequiredException si la licence ne couvre pas la fonctionnalite. */
    void requireFeature(LicenseFeature feature);

    /** Leve LicenseRequiredException si la licence est absente, expiree ou invalide. */
    void requireValid();
}
