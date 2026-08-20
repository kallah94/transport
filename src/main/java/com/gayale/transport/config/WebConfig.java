package com.gayale.transport.config;

import com.gayale.transport.license.LicenseEnforcementInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebConfig implements WebMvcConfigurer {

    private final LicenseEnforcementInterceptor licenseEnforcementInterceptor;

    public WebConfig(LicenseEnforcementInterceptor licenseEnforcementInterceptor) {
        this.licenseEnforcementInterceptor = licenseEnforcementInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Enforcement commercial : execute apres Spring Security, donc une requete non
        // authentifiee recoit 401 (et non 402). Les routes d'echappement (/auth, /api/license,
        // /api/branding, Swagger) sont gerees dans l'intercepteur lui-meme.
        registry.addInterceptor(licenseEnforcementInterceptor).addPathPatterns("/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        // Configurer la résolution des arguments Pageable avec une taille de page par défaut
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
        resolver.setMaxPageSize(100);
        resolver.setOneIndexedParameters(true);
        argumentResolvers.add(resolver);
    }

    // CORS est configuré de manière unique dans SecurityConfig.corsConfigurationSource()
    // (inclut la méthode PATCH). Ne pas redéfinir le CORS ici pour éviter les conflits.
}