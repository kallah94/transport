package com.gayale.transport.repository;

import com.gayale.transport.model.License;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface LicenseRepository extends MongoRepository<License, String> {

    /** Licence courante du tenant (le filtrage par tenant est applique par TenantAwareMongoTemplate). */
    Optional<License> findFirstByActiveTrueOrderByActivatedAtDesc();

    List<License> findByActiveTrue();

    Optional<License> findByLicenseId(String licenseId);
}
