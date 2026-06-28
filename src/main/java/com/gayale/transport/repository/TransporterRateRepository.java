package com.gayale.transport.repository;

import com.gayale.transport.model.TransporterRate;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TransporterRateRepository extends MongoRepository<TransporterRate, String> {

    List<TransporterRate> findByTransporterId(String transporterId);

    List<TransporterRate> findByActiveTrue();

    List<TransporterRate> findByTransporterIdAndActiveTrue(String transporterId);
}
