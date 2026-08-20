package com.gayale.transport.service;

import com.gayale.transport.iservices.ILicenseService;
import com.gayale.transport.license.LicenseClaims;
import com.gayale.transport.license.LicenseFeature;
import com.gayale.transport.license.LicenseKeyCodec;
import com.gayale.transport.license.LicenseKeyException;
import com.gayale.transport.license.LicensePlan;
import com.gayale.transport.license.LicenseProperties;
import com.gayale.transport.license.LicenseRequiredException;
import com.gayale.transport.license.LicenseState;
import com.gayale.transport.license.LicenseStatus;
import com.gayale.transport.license.MachineFingerprint;
import com.gayale.transport.model.License;
import com.gayale.transport.repository.LicenseRepository;
import com.gayale.transport.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Coeur du modele commercial : resout, active et fait respecter la licence.
 *
 * Principe : la base ne fait PAS foi. A chaque resolution on reverifie la signature Ed25519
 * de la cle stockee, on recompare l'empreinte machine et la date d'expiration. Un client qui
 * editerait la collection {@code licenses} pour se donner le plan ENTERPRISE verrait sa
 * licence passer INVALID a la resolution suivante.
 */
@Slf4j
@Service
public class LicenseService implements ILicenseService {

    /** Cle de cache utilisee en mode dedie (un seul tenant, tenantId parfois null). */
    private static final String DEFAULT_SCOPE = "__default__";

    private final LicenseRepository licenseRepository;
    private final LicenseProperties properties;

    /** Cache par tenant : evite de reverifier la signature a chaque requete HTTP. */
    private final Map<String, CachedState> cache = new ConcurrentHashMap<>();

    private volatile PublicKey publicKey;
    private volatile boolean publicKeyResolved;

    public LicenseService(LicenseRepository licenseRepository, LicenseProperties properties) {
        this.licenseRepository = licenseRepository;
        this.properties = properties;
    }

    // ------------------------------------------------------------------ resolution

    @Override
    public LicenseState current() {
        if (!properties.isEnforce()) {
            return LicenseState.unrestricted();
        }
        String scope = scope();
        CachedState cached = cache.get(scope);
        if (cached != null && !cached.isStale(properties.getCacheSeconds())) {
            return cached.state;
        }
        LicenseState state = resolve();
        cache.put(scope, new CachedState(state, LocalDateTime.now()));
        return state;
    }

    /** Force le recalcul (appele apres activation / retrait). */
    public void invalidateCache() {
        cache.remove(scope());
    }

    private LicenseState resolve() {
        Optional<License> stored = licenseRepository.findFirstByActiveTrueOrderByActivatedAtDesc();

        if (stored.isEmpty()) {
            License trial = startTrialIfPossible();
            if (trial == null) {
                return missing("Aucune licence installee. Saisissez la cle fournie par votre revendeur.");
            }
            stored = Optional.of(trial);
        }

        License license = stored.get();

        if (license.isTrial()) {
            return fromTrial(license);
        }

        LicenseClaims claims;
        try {
            claims = LicenseKeyCodec.verify(license.getLicenseKey(), publicKey());
        } catch (LicenseKeyException e) {
            log.warn("Licence {} rejetee : {}", LicenseKeyCodec.tail(license.getLicenseKey()), e.getMessage());
            return LicenseState.builder()
                    .status(e.getStatus())
                    .customer(license.getCustomer())
                    .message(e.getMessage())
                    .build();
        } catch (IllegalStateException e) {
            // Cle publique absente ou illisible : on refuse plutot que d'ouvrir en grand.
            log.error("Configuration de licence invalide : {}", e.getMessage());
            return LicenseState.builder()
                    .status(LicenseStatus.INVALID)
                    .message("Configuration de licence invalide sur ce poste : " + e.getMessage())
                    .build();
        }

        if (isRevoked(claims.getLicenseId())) {
            return LicenseState.builder()
                    .status(LicenseStatus.REVOKED)
                    .customer(claims.getCustomer())
                    .licenseId(claims.getLicenseId())
                    .message("Cette licence a ete revoquee. Contactez votre revendeur.")
                    .build();
        }

        if (properties.isBindMachine()
                && claims.getMachineId() != null
                && !claims.getMachineId().equalsIgnoreCase(MachineFingerprint.current())) {
            return LicenseState.builder()
                    .status(LicenseStatus.MACHINE_MISMATCH)
                    .customer(claims.getCustomer())
                    .licenseId(claims.getLicenseId())
                    .machineId(claims.getMachineId())
                    .message("Cette licence est liee a un autre poste. Empreinte de ce poste : "
                            + MachineFingerprint.current() + ". Demandez un transfert a votre revendeur.")
                    .build();
        }

        return fromClaims(claims);
    }

    private LicenseState fromClaims(LicenseClaims claims) {
        LocalDate today = LocalDate.now();
        LocalDate expiry = claims.getExpiresAt();
        LicenseStatus status;
        Integer daysRemaining = null;
        LocalDate graceUntil = null;
        String message = null;

        if (expiry == null) {
            status = LicenseStatus.ACTIVE;
        } else {
            daysRemaining = (int) ChronoUnit.DAYS.between(today, expiry);
            graceUntil = expiry.plusDays(properties.getGraceDays());
            if (!today.isAfter(expiry)) {
                status = LicenseStatus.ACTIVE;
                if (daysRemaining <= 30) {
                    message = "Votre licence expire le " + expiry + " (" + daysRemaining + " jours).";
                }
            } else if (!today.isAfter(graceUntil)) {
                status = LicenseStatus.GRACE;
                message = "Licence expiree le " + expiry + ". Periode de grace jusqu'au " + graceUntil
                        + " : renouvelez pour eviter le blocage.";
            } else {
                status = LicenseStatus.EXPIRED;
                message = "Licence expiree le " + expiry + " et periode de grace epuisee. "
                        + "Saisissez une nouvelle cle pour continuer.";
            }
        }

        return LicenseState.builder()
                .status(status)
                .plan(claims.resolvedPlan())
                .features(claims.resolvedFeatures())
                .customer(claims.getCustomer())
                .customerKey(claims.getCustomerKey())
                .licenseId(claims.getLicenseId())
                .issuedAt(claims.getIssuedAt())
                .expiresAt(expiry)
                .graceUntil(graceUntil)
                .daysRemaining(daysRemaining)
                .machineId(claims.getMachineId())
                .maxUsers(claims.effectiveMaxUsers())
                .maxTrucks(claims.effectiveMaxTrucks())
                .maxTicketsPerMonth(claims.effectiveMaxTicketsPerMonth())
                .message(message)
                .build();
    }

    private LicenseState fromTrial(License trial) {
        LocalDate expiry = trial.getExpiresAt();
        int daysRemaining = (int) ChronoUnit.DAYS.between(LocalDate.now(), expiry);
        boolean over = LocalDate.now().isAfter(expiry);
        return LicenseState.builder()
                .status(over ? LicenseStatus.EXPIRED : LicenseStatus.TRIAL)
                .plan(LicensePlan.TRIAL)
                .features(LicensePlan.TRIAL.getFeatures())
                .customer(trial.getCustomer())
                .issuedAt(trial.getIssuedAt())
                .expiresAt(expiry)
                .daysRemaining(daysRemaining)
                .trial(true)
                .maxUsers(LicensePlan.TRIAL.getMaxUsers())
                .maxTrucks(LicensePlan.TRIAL.getMaxTrucks())
                .maxTicketsPerMonth(LicensePlan.TRIAL.getMaxTicketsPerMonth())
                .message(over
                        ? "Votre periode d'essai est terminee. Saisissez une cle de licence pour continuer."
                        : "Periode d'essai : " + daysRemaining + " jours restants.")
                .build();
    }

    private LicenseState missing(String message) {
        return LicenseState.builder()
                .status(LicenseStatus.MISSING)
                .features(EnumSet.noneOf(LicenseFeature.class))
                .message(message)
                .build();
    }

    /**
     * Cree l'essai a la premiere installation. Non rejouable : une fois un essai enregistre
     * (meme expire et desactive), on n'en cree pas un second pour ce tenant.
     */
    private License startTrialIfPossible() {
        if (properties.getTrialDays() <= 0) {
            return null;
        }
        boolean alreadyUsed = licenseRepository.findAll().stream().anyMatch(License::isTrial);
        if (alreadyUsed) {
            return null;
        }
        License trial = License.builder()
                .plan(LicensePlan.TRIAL.name())
                .customer("Periode d'essai")
                .issuedAt(LocalDate.now())
                .expiresAt(LocalDate.now().plusDays(properties.getTrialDays()))
                .features(LicensePlan.TRIAL.getFeatures().stream().map(Enum::name).collect(Collectors.toSet()))
                .maxUsers(LicensePlan.TRIAL.getMaxUsers())
                .maxTrucks(LicensePlan.TRIAL.getMaxTrucks())
                .maxTicketsPerMonth(LicensePlan.TRIAL.getMaxTicketsPerMonth())
                .activatedAt(LocalDateTime.now())
                .activatedBy("system")
                .trial(true)
                .active(true)
                .build();
        licenseRepository.save(trial);
        log.info("Periode d'essai de {} jours demarree (fin le {}).",
                properties.getTrialDays(), trial.getExpiresAt());
        return trial;
    }

    // ------------------------------------------------------------------ activation

    @Override
    public License activate(String rawKey, String activatedBy) {
        LicenseClaims claims = LicenseKeyCodec.verify(rawKey, publicKey());

        if (isRevoked(claims.getLicenseId())) {
            throw new LicenseKeyException(LicenseStatus.REVOKED,
                    "Cette licence a ete revoquee et ne peut plus etre activee.");
        }
        if (properties.isBindMachine()
                && claims.getMachineId() != null
                && !claims.getMachineId().equalsIgnoreCase(MachineFingerprint.current())) {
            throw new LicenseKeyException(LicenseStatus.MACHINE_MISMATCH,
                    "Cette cle a ete emise pour un autre poste. Empreinte de ce poste : "
                            + MachineFingerprint.current());
        }
        if (claims.getExpiresAt() != null
                && LocalDate.now().isAfter(claims.getExpiresAt().plusDays(properties.getGraceDays()))) {
            throw new LicenseKeyException(LicenseStatus.EXPIRED,
                    "Cette cle a expire le " + claims.getExpiresAt() + ".");
        }

        // Une seule licence active : les precedentes sont archivees, pas supprimees.
        List<License> actives = licenseRepository.findByActiveTrue();
        for (License previous : actives) {
            previous.setActive(false);
            licenseRepository.save(previous);
        }

        License license = License.builder()
                .licenseKey(LicenseKeyCodec.normalize(rawKey))
                .licenseId(claims.getLicenseId())
                .customer(claims.getCustomer())
                .customerKey(claims.getCustomerKey())
                .contactEmail(claims.getContactEmail())
                .plan(claims.getPlan() == null ? null : claims.getPlan().toUpperCase(Locale.ROOT))
                .issuedAt(claims.getIssuedAt())
                .expiresAt(claims.getExpiresAt())
                .machineId(claims.getMachineId())
                .maxUsers(claims.effectiveMaxUsers())
                .maxTrucks(claims.effectiveMaxTrucks())
                .maxTicketsPerMonth(claims.effectiveMaxTicketsPerMonth())
                .features(claims.resolvedFeatures().stream().map(Enum::name).collect(Collectors.toCollection(java.util.LinkedHashSet::new)))
                .note(claims.getNote())
                .activatedAt(LocalDateTime.now())
                .activatedBy(activatedBy)
                .trial(false)
                .active(true)
                .build();

        License saved = licenseRepository.save(license);
        invalidateCache();
        log.info("Licence activee : client='{}', plan={}, expiration={}, id={}",
                saved.getCustomer(), saved.getPlan(), saved.getExpiresAt(), saved.getLicenseId());
        return saved;
    }

    @Override
    public void deactivate() {
        for (License active : licenseRepository.findByActiveTrue()) {
            if (active.isTrial()) {
                // On conserve la trace de l'essai pour empecher un redemarrage d'essai.
                continue;
            }
            active.setActive(false);
            licenseRepository.save(active);
        }
        invalidateCache();
        log.info("Licence desactivee sur ce deploiement.");
    }

    @Override
    public String machineId() {
        return MachineFingerprint.current();
    }

    /** Licence stockee courante (pour l'affichage detaille cote administration). */
    public Optional<License> currentRecord() {
        return licenseRepository.findFirstByActiveTrueOrderByActivatedAtDesc();
    }

    // ------------------------------------------------------------------ controles

    @Override
    public void requireValid() {
        LicenseState state = current();
        if (!state.isValid()) {
            throw new LicenseRequiredException(state.getStatus(),
                    state.getMessage() != null ? state.getMessage() : "Licence invalide.");
        }
    }

    @Override
    public void requireFeature(LicenseFeature feature) {
        requireValid();
        LicenseState state = current();
        if (!state.has(feature)) {
            throw LicenseRequiredException.feature(feature);
        }
    }

    // ------------------------------------------------------------------ interne

    private boolean isRevoked(String licenseId) {
        List<String> revoked = properties.getRevoked();
        return licenseId != null && revoked != null
                && revoked.stream().anyMatch(licenseId::equalsIgnoreCase);
    }

    private PublicKey publicKey() {
        if (!publicKeyResolved) {
            synchronized (this) {
                if (!publicKeyResolved) {
                    String raw = properties.getPublicKey();
                    if (raw == null || raw.isBlank()) {
                        throw new IllegalStateException(
                                "app.license.public-key (LICENSE_PUBLIC_KEY) n'est pas configuree.");
                    }
                    publicKey = LicenseKeyCodec.publicKeyFromBase64(raw);
                    publicKeyResolved = true;
                }
            }
        }
        return publicKey;
    }

    private String scope() {
        String tenantId = TenantContext.getTenantId();
        return tenantId == null ? DEFAULT_SCOPE : tenantId;
    }

    private record CachedState(LicenseState state, LocalDateTime at) {
        boolean isStale(int seconds) {
            return at.plusSeconds(Math.max(1, seconds)).isBefore(LocalDateTime.now());
        }
    }

    /** Exposition en lecture pour les composants d'enforcement. */
    public Set<LicenseFeature> features() {
        return current().getFeatures();
    }
}
