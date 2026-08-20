package com.gayale.transport.license;

import com.gayale.transport.repository.TruckRepository;
import com.gayale.transport.repository.UserRepository;
import com.gayale.transport.repository.WeightTicketRepository;
import com.gayale.transport.service.LicenseService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Point unique des controles de quota. Appele par les services metier AVANT une creation,
 * pour refuser proprement (HTTP 402) plutot que de laisser le client depasser son contrat.
 *
 * Un quota a -1 est illimite. Les controles sont volontairement des "creation gates" :
 * les donnees deja saisies restent lisibles meme si la licence est retrogradee, ce qui evite
 * de rendre inaccessible l'historique comptable d'un client en litige commercial.
 */
@Component
public class LicenseGuard {

    private final LicenseService licenseService;
    private final UserRepository userRepository;
    private final TruckRepository truckRepository;
    private final WeightTicketRepository weightTicketRepository;

    public LicenseGuard(LicenseService licenseService,
                        UserRepository userRepository,
                        TruckRepository truckRepository,
                        WeightTicketRepository weightTicketRepository) {
        this.licenseService = licenseService;
        this.userRepository = userRepository;
        this.truckRepository = truckRepository;
        this.weightTicketRepository = weightTicketRepository;
    }

    /** Etat courant, pour l'affichage. */
    public LicenseState state() {
        return licenseService.current();
    }

    /** Refuse si la fonctionnalite n'est pas couverte par le plan. */
    public void requireFeature(LicenseFeature feature) {
        licenseService.requireFeature(feature);
    }

    /** A appeler avant la creation d'un utilisateur. */
    public void checkUserQuota() {
        LicenseState state = licenseService.current();
        if (state.isEnforcementDisabled()) {
            return;
        }
        licenseService.requireValid();
        int max = state.getMaxUsers();
        if (max < 0) {
            return;
        }
        long current = userRepository.count();
        if (current >= max) {
            throw LicenseRequiredException.quota("utilisateurs", current, max);
        }
    }

    /** A appeler avant la creation d'un camion. */
    public void checkTruckQuota() {
        LicenseState state = licenseService.current();
        if (state.isEnforcementDisabled()) {
            return;
        }
        licenseService.requireValid();
        int max = state.getMaxTrucks();
        if (max < 0) {
            return;
        }
        long current = truckRepository.count();
        if (current >= max) {
            throw LicenseRequiredException.quota("camions", current, max);
        }
    }

    /** A appeler avant la creation d'un ticket de pesee (quota glissant sur le mois du ticket). */
    public void checkTicketQuota(LocalDate ticketDate) {
        LicenseState state = licenseService.current();
        if (state.isEnforcementDisabled()) {
            return;
        }
        licenseService.requireValid();
        int max = state.getMaxTicketsPerMonth();
        if (max < 0) {
            return;
        }
        YearMonth month = YearMonth.from(ticketDate == null ? LocalDate.now() : ticketDate);
        long current = weightTicketRepository.countByDateBetween(month.atDay(1), month.atEndOfMonth());
        if (current >= max) {
            throw LicenseRequiredException.quota("tickets pour " + month, current, max);
        }
    }

    /** Consommation courante, pour les jauges de la page Licence. */
    public LicenseUsage usage() {
        YearMonth month = YearMonth.now();
        return new LicenseUsage(
                userRepository.count(),
                truckRepository.count(),
                weightTicketRepository.countByDateBetween(month.atDay(1), month.atEndOfMonth()));
    }

    /** Compteurs bruts d'utilisation (les plafonds viennent de {@link LicenseState}). */
    public record LicenseUsage(long users, long trucks, long ticketsThisMonth) {
    }
}
