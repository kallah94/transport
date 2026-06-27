package com.gayale.transport.repository;

import com.gayale.transport.model.DriverRate;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DriverRateRepository extends MongoRepository<DriverRate, String> {

    List<DriverRate> findByActiveTrue();

    List<DriverRate> findByTransporterIdAndActiveTrue(String transporterId);
}
