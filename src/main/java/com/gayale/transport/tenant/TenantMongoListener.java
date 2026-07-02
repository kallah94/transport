package com.gayale.transport.tenant;

import com.gayale.transport.util.AuditableEntity;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Pose automatiquement le tenant courant sur chaque entite auditable avant insertion/mise a jour,
 * si elle n'en a pas deja un. Fonctionne pour toutes les sous-classes d'AuditableEntity
 * (Project, Truck, PurchaseOrder, WeightTicket, User, tarifs, paiements, ...).
 *
 * Le tenantId n'est jamais accepte depuis le corps de requete : il est toujours derive du
 * contexte serveur (TenantContext), ce qui evite toute usurpation de locataire.
 */
@Component
public class TenantMongoListener extends AbstractMongoEventListener<AuditableEntity> {

    @Override
    public void onBeforeConvert(BeforeConvertEvent<AuditableEntity> event) {
        AuditableEntity entity = event.getSource();
        if (entity == null || StringUtils.hasText(entity.getTenantId())) {
            return;
        }
        String tenantId = TenantContext.getTenantId();
        if (StringUtils.hasText(tenantId)) {
            entity.setTenantId(tenantId);
        }
    }
}
