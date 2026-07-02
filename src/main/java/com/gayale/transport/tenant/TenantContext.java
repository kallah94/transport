package com.gayale.transport.tenant;

/**
 * Porte l'identifiant du locataire (tenant) courant pour la duree d'une requete.
 * Pose par le TenantFilter, lu par le tagging d'ecriture et le filtrage de lecture.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
