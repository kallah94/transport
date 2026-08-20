package com.gayale.transport.service;

import com.gayale.transport.model.Notification;
import com.gayale.transport.model.Notification.NotificationLevel;
import com.gayale.transport.model.Notification.NotificationType;
import com.gayale.transport.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Création et lecture des notifications admin. Les hooks métier (BC, transporteur, projet,
 * paiement) appellent notify(...) ; le frontend interroge recent()/unreadCount() par polling.
 */
@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public Notification notify(NotificationType type, NotificationLevel level,
                               String title, String message, String link, String refId) {
        Notification n = Notification.builder()
                .type(type)
                .level(level)
                .title(title)
                .message(message)
                .link(link)
                .refId(refId)
                .read(false)
                .build();
        return repository.save(n);
    }

    public Notification notify(NotificationType type, NotificationLevel level, String title, String message) {
        return notify(type, level, title, message, null, null);
    }

    /** Notification destinée à un camion (app chauffeur). */
    public Notification notifyVehicle(NotificationType type, NotificationLevel level,
                                      String title, String message, String link, String refId, String vehicle) {
        Notification n = Notification.builder()
                .type(type).level(level).title(title).message(message)
                .link(link).refId(refId).vehicle(vehicle).read(false)
                .build();
        return repository.save(n);
    }

    // ----------------- Portée ADMINISTRATION (vehicle == null) -----------------

    public List<Notification> recent(boolean unreadOnly) {
        return unreadOnly
                ? repository.findTop50ByVehicleIsNullAndReadFalseOrderByCreatedAtDesc()
                : repository.findTop50ByVehicleIsNullOrderByCreatedAtDesc();
    }

    public long unreadCount() {
        return repository.countByVehicleIsNullAndReadFalse();
    }

    public void markAllRead() {
        markAll(repository.findTop50ByVehicleIsNullAndReadFalseOrderByCreatedAtDesc());
    }

    // ----------------- Portée CHAUFFEUR (par camion) -----------------

    public List<Notification> recentForVehicle(String vehicle, boolean unreadOnly) {
        return unreadOnly
                ? repository.findTop50ByVehicleAndReadFalseOrderByCreatedAtDesc(vehicle)
                : repository.findTop50ByVehicleOrderByCreatedAtDesc(vehicle);
    }

    public long unreadCountForVehicle(String vehicle) {
        return repository.countByVehicleAndReadFalse(vehicle);
    }

    public void markAllReadForVehicle(String vehicle) {
        markAll(repository.findTop50ByVehicleAndReadFalseOrderByCreatedAtDesc(vehicle));
    }

    // Ne marque lue que si la notification appartient bien au camion (sécurité).
    public void markReadForVehicle(String id, String vehicle) {
        repository.findById(id).ifPresent(n -> {
            if (vehicle.equals(n.getVehicle()) && !n.isRead()) {
                n.setRead(true);
                repository.save(n);
            }
        });
    }

    // ----------------- Commun -----------------

    public void markRead(String id) {
        repository.findById(id).ifPresent(n -> {
            if (!n.isRead()) {
                n.setRead(true);
                repository.save(n);
            }
        });
    }

    private void markAll(List<Notification> unread) {
        unread.forEach(n -> n.setRead(true));
        if (!unread.isEmpty()) {
            repository.saveAll(unread);
        }
    }
}
