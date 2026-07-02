package com.gayale.transport.repository;

import com.gayale.transport.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findTop50ByOrderByCreatedAtDesc();

    List<Notification> findTop50ByReadFalseOrderByCreatedAtDesc();

    long countByReadFalse();
}
