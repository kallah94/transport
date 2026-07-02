package com.gayale.transport.controller;

import com.gayale.transport.model.Notification;
import com.gayale.transport.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * API des notifications admin (cloche + polling). Réservé aux rôles ADMIN / SUPER_ADMIN.
 */
@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Notifications", description = "Notifications administrateur")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Liste des 50 dernières notifications (toutes ou non-lues)")
    public List<Notification> list(@RequestParam(value = "unread", defaultValue = "false") boolean unread) {
        return service.recent(unread);
    }

    @GetMapping("/count")
    @Operation(summary = "Nombre de notifications non-lues")
    public Map<String, Long> count() {
        return Map.of("unread", service.unreadCount());
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Marque une notification comme lue")
    public ResponseEntity<Void> markRead(@PathVariable String id) {
        service.markRead(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    @Operation(summary = "Marque toutes les notifications comme lues")
    public ResponseEntity<Void> markAllRead() {
        service.markAllRead();
        return ResponseEntity.noContent().build();
    }
}
