package com.gayale.transport.repository;

import java.util.List;
import java.util.Optional;

import com.gayale.transport.model.Truck;
import com.gayale.transport.model.TransporterEnterprise;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TruckRepository extends MongoRepository<Truck, String> {

    // Find by transporter object
    List<Truck> findByTransporter(TransporterEnterprise transporter);

    // Find by transporter ID
    List<Truck> findByTransporterId(String transporterId);

    // Find by transporter name
    List<Truck> findByTransporterName(String transporterName);

    Optional<Truck> findByVehicle(String vehicle);

    Optional<Truck> findByPhone(String phone);

    boolean existsByVehicle(String vehicle);
}