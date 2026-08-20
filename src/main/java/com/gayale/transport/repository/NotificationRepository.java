package com.gayale.transport.repository;

import com.gayale.transport.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    // Notifications administration (non ciblées camion : vehicle == null).
    List<Notification> findTop50ByVehicleIsNullOrderByCreatedAtDesc();

    List<Notification> findTop50ByVehicleIsNullAndReadFalseOrderByCreatedAtDesc();

    long countByVehicleIsNullAndReadFalse();

    // Notifications destinées à un camion (app chauffeur).
    List<Notification> findTop50ByVehicleOrderByCreatedAtDesc(String vehicle);

    List<Notification> findTop50ByVehicleAndReadFalseOrderByCreatedAtDesc(String vehicle);

    long countByVehicleAndReadFalse(String vehicle);
}
