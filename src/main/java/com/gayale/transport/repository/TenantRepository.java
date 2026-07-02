package com.gayale.transport.repository;

import com.gayale.transport.model.Tenant;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TenantRepository extends MongoRepository<Tenant, String> {

    Optional<Tenant> findByKey(String key);

    Optional<Tenant> findByKeyAndActiveTrue(String key);

    boolean existsByKey(String key);
}
