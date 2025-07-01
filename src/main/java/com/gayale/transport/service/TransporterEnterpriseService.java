package com.gayale.transport.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.gayale.transport.dto.UserDto;
import com.gayale.transport.dto.transporterEntreprise.TransporterEnterpriseRequest;
import com.gayale.transport.dto.transporterEntreprise.TransporterEnterpriseResponse;
import com.gayale.transport.dto.transporterEntreprise.TransporterStatistics;
import com.gayale.transport.exception.DuplicateResourceException;
import com.gayale.transport.exception.ResourceNotFoundException;
import com.gayale.transport.model.TransporterEnterprise;
import com.gayale.transport.model.User;
import com.gayale.transport.repository.TransporterEnterpriseRepository;
import com.gayale.transport.repository.TruckRepository;
import com.gayale.transport.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@AllArgsConstructor
public class TransporterEnterpriseService {

    private final TransporterEnterpriseRepository transporterEnterpriseRepository;
    private final UserRepository userRepository;
    private final TruckRepository truckRepository;
    private final ModelMapper modelMapper;

    public TransporterEnterpriseResponse createTransporter(TransporterEnterpriseRequest request) {
        if (transporterEnterpriseRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Une entreprise avec ce nom existe déjà : " + request.getName());
        }

        if(request.getRegistrationNumber() != null && transporterEnterpriseRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new DuplicateResourceException("Une entreprise avec ce matricule existe déjà : " + request.getRegistrationNumber());
        }

        User representative = userRepository.findById(request.getRepresentativeId())
                                            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'ID : " + request.getRepresentativeId()));

        TransporterEnterprise transporter = modelMapper.map(request, TransporterEnterprise.class);
        transporter.setRepresentative(representative);
        transporter.setActive(request.getActive() != null ? request.getActive() : true);

        TransporterEnterprise savedTransporter = transporterEnterpriseRepository.save(transporter);
        return convertToResponse(savedTransporter);
    }

    public TransporterEnterpriseResponse updateTransporter(String transporterId, TransporterEnterpriseRequest request) {
        TransporterEnterprise existingTransporter = transporterEnterpriseRepository.findById(transporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Entreprise de transport introuvable avec l'ID : " + transporterId));


        if(!existingTransporter.getName().equalsIgnoreCase(request.getName()) &&
           transporterEnterpriseRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Une entreprise avec ce nom existe déjà : " + request.getName());
        }

        if (request.getRegistrationNumber() != null &&
            !existingTransporter.getRegistrationNumber().equals(request.getRegistrationNumber()) &&
            transporterEnterpriseRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new DuplicateResourceException("Une entreprise avec ce matricule existe déjà : " + request.getRegistrationNumber());
        }

        if(!existingTransporter.getRepresentative().getId().equals(request.getRepresentativeId())) {
            User representative = userRepository.findById(request.getRepresentativeId())
                                                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'ID : " + request.getRepresentativeId()));
            existingTransporter.setRepresentative(representative);
        }

        // Mettre à jour les champs modifiables
        modelMapper.map(request, existingTransporter);

        if (!existingTransporter.getRepresentative().getId().equals(request.getRepresentativeId())) {
            User representative = userRepository.findById(request.getRepresentativeId())
                                                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'ID : " + request.getRepresentativeId()));
            existingTransporter.setRepresentative(representative);
        }

        if (request.getActive() != null) {
            existingTransporter.setActive(request.getActive());
        }

        TransporterEnterprise updatedTransporter = transporterEnterpriseRepository.save(existingTransporter);
        return convertToResponse(updatedTransporter);
    }


    @Transactional(readOnly = true)
    public TransporterEnterpriseResponse getTransporterById(String transporterId) {
        TransporterEnterprise transporterEnterprise = transporterEnterpriseRepository.findById(transporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Entreprise de transport introuvable avec l'ID : " + transporterId));
        return convertToResponse(transporterEnterprise);
    }

    @Transactional(readOnly = true)
    public TransporterEnterpriseResponse getTransporterByRegistrationNumber(String registrationNumber) {
        TransporterEnterprise transporterEnterprise = transporterEnterpriseRepository.findByRegistrationNumber(registrationNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Entreprise de transport introuvable avec le matricule : " + registrationNumber));
        return convertToResponse(transporterEnterprise);
    }

    @Transactional(readOnly = true)
    public TransporterEnterpriseResponse getTransporterByName(String name) {
        TransporterEnterprise transporterEnterprise = transporterEnterpriseRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new ResourceNotFoundException("Entreprise de transport introuvable avec le nom : " + name));
        return convertToResponse(transporterEnterprise);
    }

    @Transactional(readOnly = true)
    public Page<TransporterEnterpriseResponse> getAllTransporters(Pageable pageable) {
        Page<TransporterEnterprise> transportersPage = transporterEnterpriseRepository.findAll(pageable);
        return transportersPage.map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public List<TransporterEnterpriseResponse> getActiveTransporters() {
        List<TransporterEnterprise> transporters = transporterEnterpriseRepository.findByActiveTrue();
        return transporters.stream()
                           .map(this::convertToResponse)
                           .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TransporterEnterpriseResponse> searchTransporters(String keyword, Pageable pageable) {
        Page<TransporterEnterprise> transporters = transporterEnterpriseRepository.searchByKeyword(keyword, pageable);
        return transporters.map(this::convertToResponse);
    }

    public void deactivateTransporter(String id) {
        TransporterEnterprise transporter = transporterEnterpriseRepository.findById(id)
                                                                 .orElseThrow(() -> new ResourceNotFoundException("Entreprise de transport introuvable avec l'ID : " + id));
        transporter.setActive(false);
        transporterEnterpriseRepository.save(transporter);
    }

    public void reactivateTransporter(String id) {
        TransporterEnterprise transporter = transporterEnterpriseRepository.findById(id)
                                                                 .orElseThrow(() -> new ResourceNotFoundException("Entreprise de transport introuvable avec l'ID : " + id));
        transporter.setActive(true);
        transporterEnterpriseRepository.save(transporter);
    }

    public void deleteTransporter(String id) {
        if (!transporterEnterpriseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Entreprise de transport introuvable avec l'ID : " + id);
        }
        transporterEnterpriseRepository.deleteById(id);
    }

    /**
     * Récupère les statistiques des entreprises de transport
     */
    @Transactional(readOnly = true)
    public TransporterStatistics getTransporterStatistics() {
        long totalTransporters = transporterEnterpriseRepository.count();
        long activeTransporters = transporterEnterpriseRepository.countByActiveTrue();

        List<TransporterEnterprise> activeTrucks = transporterEnterpriseRepository.findActiveTrucksOnly();
        int totalTrucks = activeTrucks.stream()
                                      .mapToInt(t -> t.getNumberOfTrucks() != null ? t.getNumberOfTrucks() : 0)
                                      .sum();

        List<TransporterEnterprise> recentlyCreated = transporterEnterpriseRepository.findRecentlyCreated(
                LocalDateTime.now().minusDays(30));

        return new TransporterStatistics(totalTransporters, activeTransporters, totalTrucks, recentlyCreated.size());
    }


    private TransporterEnterpriseResponse convertToResponse(TransporterEnterprise transporter) {
        TransporterEnterpriseResponse response = modelMapper.map(transporter, TransporterEnterpriseResponse.class);

        // Mapper manuellement le représentant pour éviter la récursion et optimiser
        if (transporter.getRepresentative() != null) {
            User rep = transporter.getRepresentative();
            UserDto userSummary = modelMapper.map(rep, UserDto.class);
            response.setRepresentative(userSummary);
        }

        return response;
    }
}
