package com.gayale.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Identite visuelle d'un tenant, servie au frontend (forme alignee sur le BrandingService Angular). */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BrandingDto {

    private String key;
    private String title;
    private String subtitle;
    private String logoUrl;
    private String faviconUrl;
    private ThemeDto theme;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ThemeDto {
        private String primary;
        private String primaryDark;
        private String accent;
        private String background;
        private String surface;
    }
}
