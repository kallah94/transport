package com.gayale.transport.repository;

import java.util.List;
import java.util.Optional;

import com.gayale.transport.model.TransporterEnterprise;
import com.gayale.transport.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface TransporterEnterpriseRepository extends MongoRepository<TransporterEnterprise, String> {
    /**
    * Trouve une enterprise de transport par son nom (insensible à la case)
    * @param name le nom de enterprise de transport
    */
    Optional<TransporterEnterprise> findByNameIgnoreCase(String name);

    /**
     * Vérifier si une enterprise de transport exist par son nom (insensible à la case)
     * @param name le nom de l'entreprise de transport
    */
    boolean existsByNameIgnoreCase(String name);


    /**
     * Trouve toutes les entreprises par statut d'activité avec pagination
     * @param active le statut d'activité de l'entreprise (true pour actif, false pour inactif)
     */
    Page<TransporterEnterprise> findByActive(boolean active, Pageable pageable);

    /**
     * Trouve les entreprises par représentant
     * @param representative le représentant de l'entreprise
     */
    List<TransporterEnterprise> findByRepresentative(User representative);

    /**
     * Trouver les entreprises dont le nom contient le texte recherché (insensible à la casse)
     * @param searchText le texte à rechercher dans le nom de l'entreprise
     */
    @Query("{'name': {$regex: ?0, $options: 'i'}}")
    Page<TransporterEnterprise> findByNameContainingIgnoreCase(String searchText, Pageable pageable);

    /**
     * Trouve les entreprises par nombre de camions (supérieur ou égal)
     */
    List<TransporterEnterprise> findByNumberOfTrucksGreaterThanEqual(Integer numberOfTrucks);

    /**
     * Trouve les entreprises par ville dans l'adresse
     */
    @Query("{'address': {$regex: ?0, $options: 'i'}}")
    List<TransporterEnterprise> findByAddressContainingIgnoreCase(String city);

    /**
     * Recherche globale dans nom, adresse et numéro d'immatriculation
     */
    @Query("{$or: [" +
            "{'name': {$regex: ?0, $options: 'i'}}, " +
            "{'address': {$regex: ?0, $options: 'i'}}, " +
            "{'registrationNumber': {$regex: ?0, $options: 'i'}}" +
            "]}")
    Page<TransporterEnterprise> searchByKeyword(String keyword, Pageable pageable);

    /**
     * Trouve les entreprises avec pagination et tri
     */
    Page<TransporterEnterprise> findAll(Pageable pageable);

    /**
     * Compte le nombre total d'entreprises actives
     */
    long countByActiveTrue();

    List<TransporterEnterprise> findByActiveTrue();
    /**
     * Calcule la somme totale des camions pour toutes les entreprises actives
     */
    @Query(value = "{'active': true}", fields = "{'numberOfTrucks': 1}")
    List<TransporterEnterprise> findActiveTrucksOnly();

    /**
     * Trouve les entreprises par numéro d'immatriculation
     */
    Optional<TransporterEnterprise> findByRegistrationNumber(String registrationNumber);

    /**
     * Vérifie si le numéro d'immatriculation existe déjà
     */
    boolean existsByRegistrationNumber(String registrationNumber);

    /**
     * Trouve les entreprises créées après une certaine date
     */
    @Query("{'createdAt': {$gte: ?0}}")
    List<TransporterEnterprise> findRecentlyCreated(java.time.LocalDateTime since);
}
