package com.gayale.transport.model;

import com.gayale.transport.util.AuditableEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Licence installee sur ce deploiement (une seule active par tenant).
 *
 * Les champs metier sont recopies depuis les claims signees au moment de l'activation :
 * c'est une COPIE DE LECTURE, pratique pour l'affichage et les requetes. La verite reste
 * {@link #licenseKey} : elle est reverifiee cryptographiquement a chaque resolution, donc
 * modifier ces champs directement en base ne debloque rien.
 *
 * Herite d'AuditableEntity : le tenantId est pose automatiquement, donc en mode partage
 * chaque client a sa propre licence sans code supplementaire.
 */
@Document(collection = "licenses")
@CompoundIndex(name = "idx_tenant_active", def = "{'tenantId': 1, 'active': 1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class License extends AuditableEntity {

    @Id
    private String id;

    /** Cle signee telle que remise au client. Absente pour une periode d'essai locale. */
    private String licenseKey;

    /** Identifiant de licence issu des claims (sert a la revocation). */
    private String licenseId;

    private String customer;
    private String customerKey;
    private String contactEmail;

    /** Nom du plan ({@code STARTER}, {@code PRO}, {@code ENTERPRISE}, {@code TRIAL}). */
    private String plan;

    private LocalDate issuedAt;

    /** null = perpetuelle. */
    private LocalDate expiresAt;

    /** Empreinte machine imposee par la cle, ou null si licence flottante. */
    private String machineId;

    private Integer maxUsers;
    private Integer maxTrucks;
    private Integer maxTicketsPerMonth;

    /** Socle du plan + add-ons, resolu a l'activation (pour affichage/audit). */
    private Set<String> features;

    private String note;

    private LocalDateTime activatedAt;

    /** Nom d'utilisateur ayant saisi la cle. */
    private String activatedBy;

    /** Vrai pour la periode d'essai auto-generee a la premiere installation. */
    @Builder.Default
    private boolean trial = false;

    /** Une seule licence active par tenant ; les precedentes sont conservees pour l'historique. */
    @Builder.Default
    private boolean active = true;
}
