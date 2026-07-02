package com.gayale.transport.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Un client de la plateforme (locataire). En mode "dedicated" il en existe un seul,
 * seme au demarrage ; en mode "shared" il en existe N, identifies par leur cle = sous-domaine.
 */
@Document(collection = "tenants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    private String id;

    /** Cle unique = sous-domaine (ex: "clientA" pour clientA.app.com). */
    @Indexed(unique = true)
    private String key;

    private String title;
    private String logoUrl;
    private Theme theme;

    @Builder.Default
    private boolean active = true;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Theme {
        private String primary;
        private String primaryDark;
        private String accent;
        private String background;
        private String surface;
    }
}
