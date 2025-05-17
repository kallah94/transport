package com.gayale.transport.repository;

import java.util.List;
import java.util.Optional;

import com.gayale.transport.model.Truck;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TruckRepository extends MongoRepository<Truck, String> {

    List<Truck> findByTransporter(String transporter);

    Optional<Truck> findByVehicle(String vehicle);

    Optional<Truck> findByPhone(String phone);

    boolean existsByVehicle(String vehicle);
}
