package com.gayale.transport.tenant;

import com.gayale.transport.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resout le locataire (tenant) courant pour chaque requete et le pose dans {@link TenantContext}.
 *  - mode "dedicated" : tenant constant = app.tenant.default-key
 *  - mode "shared"    : tenant deduit du sous-domaine de l'en-tete Host (clientA.app.com -> clientA)
 * Le contexte est systematiquement vide en fin de requete (reutilisation des threads).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepository;
    private final Map<String, String> keyToId = new ConcurrentHashMap<>();

    @Value("${app.mode:dedicated}")
    private String mode;

    @Value("${app.tenant.default-key:default}")
    private String defaultKey;

    public TenantFilter(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String key = isShared() ? subdomainOf(request.getServerName()) : defaultKey;
            TenantContext.setTenantId(resolveTenantId(key));
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean isShared() {
        return "shared".equalsIgnoreCase(mode);
    }

    private String resolveTenantId(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        String cached = keyToId.get(key);
        if (cached != null) {
            return cached;
        }
        return tenantRepository.findByKeyAndActiveTrue(key)
                .map(t -> {
                    keyToId.put(key, t.getId());
                    return t.getId();
                })
                .orElse(null);
    }

    /** clientA.app.com -> "clientA" ; "localhost" / IP -> null. */
    private String subdomainOf(String host) {
        if (!StringUtils.hasText(host)) {
            return null;
        }
        int dot = host.indexOf('.');
        if (dot <= 0) {
            return null;
        }
        return host.substring(0, dot);
    }
}
