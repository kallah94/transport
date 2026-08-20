package com.gayale.transport.license;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration du module de licence (prefixe {@code app.license} dans application.yml).
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.license")
public class LicenseProperties {

    /**
     * Cle PUBLIQUE Ed25519 de l'editeur, encodage X.509 en base64.
     * En production, fournie par la variable d'environnement LICENSE_PUBLIC_KEY.
     */
    private String publicKey;

    /**
     * Coupe totalement l'enforcement (developpement / tests d'integration).
     * Doit rester {@code true} sur les livraisons client.
     */
    private boolean enforce = true;

    /** Jours d'utilisation toleres apres expiration, avant blocage. */
    private int graceDays = 14;

    /** Duree de l'essai auto-genere a la premiere installation. 0 = pas d'essai. */
    private int trialDays = 30;

    /**
     * Refuse une cle liee a une autre machine. Mettre a {@code false} sur un serveur
     * web partage ou dans un conteneur dont l'empreinte varie a chaque redemarrage.
     */
    private boolean bindMachine = true;

    /**
     * Identifiants de licence revoques (champ {@code id} des claims). Permet de neutraliser
     * une cle diffusee sans attendre son expiration, via une mise a jour de l'application.
     */
    private List<String> revoked = new ArrayList<>();

    /** Duree du cache de resolution, en secondes (evite de reverifier la signature a chaque requete). */
    private int cacheSeconds = 60;
}
