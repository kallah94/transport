package com.gayale.transport.tenant;

import com.gayale.transport.model.Tenant;
import com.gayale.transport.repository.TenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Garantit l'existence d'un tenant par defaut au demarrage (indispensable en mode dedie,
 * pratique en mode partage). Idempotent : ne cree rien si la cle existe deja.
 */
@Slf4j
@Component
public class TenantSeeder implements ApplicationRunner {

    private final TenantRepository tenantRepository;

    @Value("${app.tenant.default-key:default}")
    private String defaultKey;

    public TenantSeeder(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (tenantRepository.existsByKey(defaultKey)) {
            return;
        }
        Tenant tenant = Tenant.builder()
                .key(defaultKey)
                .title("Gayale Transport")
                .active(true)
                .theme(new Tenant.Theme("#3f51b5", "#1a237e", "#ff4081", "#121212", "#1e1e1e"))
                .build();
        tenantRepository.save(tenant);
        log.info("Tenant par defaut cree : key='{}'", defaultKey);
    }
}
