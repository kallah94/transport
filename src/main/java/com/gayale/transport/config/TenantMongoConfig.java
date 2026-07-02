package com.gayale.transport.config;

import com.gayale.transport.tenant.TenantAwareMongoTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

/**
 * Remplace le MongoTemplate par defaut par une version tenant-aware.
 * Le filtrage par tenant n'est actif qu'en mode "shared" ; en "dedicated" c'est un passe-plat.
 */
@Configuration
public class TenantMongoConfig {

    @Bean
    @Primary
    public MongoTemplate mongoTemplate(MongoDatabaseFactory factory,
                                       MappingMongoConverter converter,
                                       @Value("${app.mode:dedicated}") String mode) {
        return new TenantAwareMongoTemplate(factory, converter, "shared".equalsIgnoreCase(mode));
    }
}
