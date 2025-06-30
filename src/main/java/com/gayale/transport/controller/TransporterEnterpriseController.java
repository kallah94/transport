package com.gayale.transport.controller;

import java.util.List;

import com.gayale.transport.dto.transporterEntreprise.TransporterEnterpriseRequest;
import com.gayale.transport.dto.transporterEntreprise.TransporterEnterpriseResponse;
import com.gayale.transport.dto.transporterEntreprise.TransporterStatistics;
import com.gayale.transport.service.TransporterEnterpriseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transporter-enterprises")
@SecurityRequirement(name = "JWT")
@Tag(name = "Entreprises de Transport", description = "Gestion des entreprises de transport et de leurs flottes")
public class TransporterEnterpriseController {

    private final TransporterEnterpriseService transporterEnterpriseService;

    @Autowired
    public TransporterEnterpriseController(TransporterEnterpriseService transporterEnterpriseService) {
        this.transporterEnterpriseService = transporterEnterpriseService;
    }

    @Operation(summary = "Créer une nouvelle entreprise de transport",
            description = "Crée une nouvelle entreprise de transport avec son représentant et sa flotte")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Entreprise créée avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "409", description = "Entreprise avec ce nom existe déjà"),
            @ApiResponse(responseCode = "401", description = "Non autorisé"),
            @ApiResponse(responseCode = "403", description = "Accès interdit")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    public ResponseEntity<TransporterEnterpriseResponse> createTransporter(
            @Valid @RequestBody TransporterEnterpriseRequest request) {
        TransporterEnterpriseResponse response = transporterEnterpriseService.createTransporter(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Récupérer toutes les entreprises de transport",
            description = "Récupère la liste paginée de toutes les entreprises de transport")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('GUEST')")
    public ResponseEntity<Page<TransporterEnterpriseResponse>> getAllTransporters(
            @Parameter(description = "Numéro de page (0-indexé)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Champ de tri")
            @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Direction du tri (asc ou desc)")
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<TransporterEnterpriseResponse> transporters = transporterEnterpriseService.getAllTransporters(pageable);
        return ResponseEntity.ok(transporters);
    }

    @Operation(summary = "Récupérer les entreprises actives",
            description = "Récupère la liste de toutes les entreprises de transport actives")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé")
    })
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('GUEST')")
    public ResponseEntity<List<TransporterEnterpriseResponse>> getActiveTransporters() {
        List<TransporterEnterpriseResponse> transporters = transporterEnterpriseService.getActiveTransporters();
        return ResponseEntity.ok(transporters);
    }

    @Operation(summary = "Rechercher des entreprises",
            description = "Recherche des entreprises par nom, adresse ou numéro d'immatriculation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Résultats de recherche récupérés"),
            @ApiResponse(responseCode = "401", description = "Non autorisé")
    })
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('GUEST')")
    public ResponseEntity<Page<TransporterEnterpriseResponse>> searchTransporters(
            @Parameter(description = "Mot-clé de recherche")
            @RequestParam String keyword,
            @Parameter(description = "Numéro de page (0-indexé)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page")
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<TransporterEnterpriseResponse> transporters = transporterEnterpriseService.searchTransporters(keyword, pageable);
        return ResponseEntity.ok(transporters);
    }

    @Operation(summary = "Récupérer une entreprise par ID",
            description = "Récupère les détails d'une entreprise de transport spécifique")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entreprise trouvée"),
            @ApiResponse(responseCode = "404", description = "Entreprise non trouvée"),
            @ApiResponse(responseCode = "401", description = "Non autorisé")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('GUEST')")
    public ResponseEntity<TransporterEnterpriseResponse> getTransporterById(
            @Parameter(description = "ID de l'entreprise")
            @PathVariable String id) {
        TransporterEnterpriseResponse transporter = transporterEnterpriseService.getTransporterById(id);
        return ResponseEntity.ok(transporter);
    }

    @Operation(summary = "Mettre à jour une entreprise",
            description = "Met à jour les informations d'une entreprise de transport existante")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entreprise mise à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Entreprise non trouvée"),
            @ApiResponse(responseCode = "409", description = "Conflit avec une autre entreprise"),
            @ApiResponse(responseCode = "401", description = "Non autorisé"),
            @ApiResponse(responseCode = "403", description = "Accès interdit")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    public ResponseEntity<TransporterEnterpriseResponse> updateTransporter(
            @Parameter(description = "ID de l'entreprise")
            @PathVariable String id,
            @Valid @RequestBody TransporterEnterpriseRequest request) {
        TransporterEnterpriseResponse response = transporterEnterpriseService.updateTransporter(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Désactiver une entreprise",
            description = "Désactive une entreprise de transport (suppression logique)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Entreprise désactivée avec succès"),
            @ApiResponse(responseCode = "404", description = "Entreprise non trouvée"),
            @ApiResponse(responseCode = "401", description = "Non autorisé"),
            @ApiResponse(responseCode = "403", description = "Accès interdit")
    })
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateTransporter(
            @Parameter(description = "ID de l'entreprise")
            @PathVariable String id) {
        transporterEnterpriseService.deactivateTransporter(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Réactiver une entreprise",
            description = "Réactive une entreprise de transport précédemment désactivée")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Entreprise réactivée avec succès"),
            @ApiResponse(responseCode = "404", description = "Entreprise non trouvée"),
            @ApiResponse(responseCode = "401", description = "Non autorisé"),
            @ApiResponse(responseCode = "403", description = "Accès interdit")
    })
    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reactivateTransporter(
            @Parameter(description = "ID de l'entreprise")
            @PathVariable String id) {
        transporterEnterpriseService.reactivateTransporter(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Supprimer définitivement une entreprise",
            description = "Supprime définitivement une entreprise de transport de la base de données")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Entreprise supprimée avec succès"),
            @ApiResponse(responseCode = "404", description = "Entreprise non trouvée"),
            @ApiResponse(responseCode = "401", description = "Non autorisé"),
            @ApiResponse(responseCode = "403", description = "Accès interdit")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTransporter(
            @Parameter(description = "ID de l'entreprise")
            @PathVariable String id) {
        transporterEnterpriseService.deleteTransporter(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Obtenir les statistiques des transporteurs",
            description = "Récupère les statistiques générales sur les entreprises de transport")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistiques récupérées avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé")
    })
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    public ResponseEntity<TransporterStatistics> getStatistics() {
        TransporterStatistics stats = transporterEnterpriseService.getTransporterStatistics();
        return ResponseEntity.ok(stats);
    }
}
