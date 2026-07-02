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

    public List<Notification> recent(boolean unreadOnly) {
        return unreadOnly
                ? repository.findTop50ByReadFalseOrderByCreatedAtDesc()
                : repository.findTop50ByOrderByCreatedAtDesc();
    }

    public long unreadCount() {
        return repository.countByReadFalse();
    }

    public void markRead(String id) {
        repository.findById(id).ifPresent(n -> {
            if (!n.isRead()) {
                n.setRead(true);
                repository.save(n);
            }
        });
    }

    public void markAllRead() {
        List<Notification> unread = repository.findTop50ByReadFalseOrderByCreatedAtDesc();
        unread.forEach(n -> n.setRead(true));
        if (!unread.isEmpty()) {
            repository.saveAll(unread);
        }
    }
}
