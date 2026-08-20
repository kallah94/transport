package com.gayale.transport.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Contenu signe d'une cle de licence. C'est la source de verite : tout ce qui est
 * facture au client est ici, et rien de tout cela n'est modifiable sans invalider la signature.
 *
 * Les noms de champs sont volontairement courts (la cle est saisie a la main par le client)
 * et STABLES : ne jamais les renommer, sous peine d'invalider les licences deja emises.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LicenseClaims {

    /** Version du format de payload. */
    @JsonProperty("v")
    @Builder.Default
    private int version = 1;

    /** Produit vise. Empeche d'utiliser une cle d'un autre logiciel de l'editeur. */
    @JsonProperty("pr")
    private String product;

    /** Identifiant unique de la licence (sert aussi de cle de revocation). */
    @JsonProperty("id")
    private String licenseId;

    /** Raison sociale du client, affichee dans l'application. */
    @JsonProperty("cu")
    private String customer;

    /** Cle technique du client (= cle de tenant / de packaging white-label). */
    @JsonProperty("ck")
    private String customerKey;

    /** Contact commercial / e-mail du signataire du contrat. */
    @JsonProperty("em")
    private String contactEmail;

    /** Plan souscrit. */
    @JsonProperty("pl")
    private String plan;

    /** Date d'emission. */
    @JsonProperty("iat")
    private LocalDate issuedAt;

    /** Date d'expiration (incluse). null = licence perpetuelle. */
    @JsonProperty("exp")
    private LocalDate expiresAt;

    /** Empreinte machine autorisee. null = licence non liee (flottante). */
    @JsonProperty("mid")
    private String machineId;

    /** Surcharge du quota utilisateurs du plan. null = valeur du plan. -1 = illimite. */
    @JsonProperty("lu")
    private Integer maxUsers;

    /** Surcharge du quota camions. */
    @JsonProperty("lt")
    private Integer maxTrucks;

    /** Surcharge du quota de tickets par mois. */
    @JsonProperty("lm")
    private Integer maxTicketsPerMonth;

    /** Fonctionnalites ajoutees au socle du plan (add-ons vendus separement). */
    @JsonProperty("ft")
    private Set<String> extraFeatures;

    /** Note libre (n. de commande, remarque contractuelle). */
    @JsonProperty("no")
    private String note;

    /** Plan resolu, ou null si le libelle est inconnu de cette version du logiciel. */
    public LicensePlan resolvedPlan() {
        return LicensePlan.from(plan);
    }

    /** Socle du plan + add-ons. Les libelles inconnus sont ignores (compatibilite ascendante). */
    public Set<LicenseFeature> resolvedFeatures() {
        Set<LicenseFeature> out = new LinkedHashSet<>();
        LicensePlan p = resolvedPlan();
        if (p != null) {
            out.addAll(p.getFeatures());
        }
        if (extraFeatures != null) {
            for (String f : extraFeatures) {
                if (f == null) {
                    continue;
                }
                try {
                    out.add(LicenseFeature.valueOf(f.trim().toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                    // Add-on inconnu de cette version : ignore plutot que de casser la licence.
                }
            }
        }
        return out;
    }

    public int effectiveMaxUsers() {
        return maxUsers != null ? maxUsers : (resolvedPlan() != null ? resolvedPlan().getMaxUsers() : 0);
    }

    public int effectiveMaxTrucks() {
        return maxTrucks != null ? maxTrucks : (resolvedPlan() != null ? resolvedPlan().getMaxTrucks() : 0);
    }

    public int effectiveMaxTicketsPerMonth() {
        return maxTicketsPerMonth != null
                ? maxTicketsPerMonth
                : (resolvedPlan() != null ? resolvedPlan().getMaxTicketsPerMonth() : 0);
    }
}
