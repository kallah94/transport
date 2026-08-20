package com.gayale.transport.controller;

import com.gayale.transport.dto.driver.DriverDto;
import com.gayale.transport.model.Notification;
import com.gayale.transport.repository.UserRepository;
import com.gayale.transport.service.DriverService;
import com.gayale.transport.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * API de l'application mobile chauffeur. Réservée au rôle DRIVER ; toutes les données sont
 * restreintes au camion (`vehicle`) rattaché au compte authentifié — jamais un paramètre client.
 */
@RestController
@RequestMapping("/api/driver")
@PreAuthorize("hasRole('DRIVER')")
@Tag(name = "Driver", description = "API mobile chauffeur (données du camion)")
public class DriverController {

    private final DriverService driverService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public DriverController(DriverService driverService, UserRepository userRepository,
                           NotificationService notificationService) {
        this.driverService = driverService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // Camion du compte authentifié (résolu côté serveur, jamais fourni par le client).
    private String currentVehicle() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null) ? auth.getName() : null;
        String vehicle = (username == null) ? null
                : userRepository.findByUsername(username).map(u -> u.getVehicle()).orElse(null);
        if (vehicle == null || vehicle.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Compte chauffeur non rattaché à un camion.");
        }
        return vehicle;
    }

    @GetMapping("/me")
    @Operation(summary = "Profil du camion + statistiques rapides")
    public DriverDto.Profile me() {
        return driverService.profile(currentVehicle());
    }

    @GetMapping("/trips")
    @Operation(summary = "Trajets du camion (tickets), filtrables par période et projet")
    public List<DriverDto.Trip> trips(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String projectId) {
        return driverService.trips(currentVehicle(), from, to, projectId);
    }

    @GetMapping("/projects")
    @Operation(summary = "Projets où le camion est intervenu + progression + part du camion")
    public List<DriverDto.Project> projects() {
        return driverService.projects(currentVehicle());
    }

    @GetMapping("/stats")
    @Operation(summary = "Statistiques du camion (tonnage & trajets par projet et par mois)")
    public DriverDto.Stats stats() {
        return driverService.stats(currentVehicle());
    }

    @GetMapping("/notifications")
    @Operation(summary = "Notifications du camion (toutes ou non-lues)")
    public List<Notification> notifications(@RequestParam(value = "unread", defaultValue = "false") boolean unread) {
        return notificationService.recentForVehicle(currentVehicle(), unread);
    }

    @GetMapping("/notifications/count")
    @Operation(summary = "Nombre de notifications non-lues du camion")
    public Map<String, Long> notificationsCount() {
        return Map.of("unread", notificationService.unreadCountForVehicle(currentVehicle()));
    }

    @PutMapping("/notifications/{id}/read")
    @Operation(summary = "Marque une notification du camion comme lue")
    public ResponseEntity<Void> markNotificationRead(@PathVariable String id) {
        notificationService.markReadForVehicle(id, currentVehicle());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/notifications/read-all")
    @Operation(summary = "Marque toutes les notifications du camion comme lues")
    public ResponseEntity<Void> markAllNotificationsRead() {
        notificationService.markAllReadForVehicle(currentVehicle());
        return ResponseEntity.noContent().build();
    }
}
