package com.gayale.transport.repository;

import com.gayale.transport.model.FuelConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface FuelConfigRepository extends MongoRepository<FuelConfig, String> {

    Optional<FuelConfig> findFirstByActiveTrueOrderByEffectiveFromDesc();
}
