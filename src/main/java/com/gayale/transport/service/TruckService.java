package com.gayale.transport.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.gayale.transport.dto.truck.TruckRequest;
import com.gayale.transport.dto.truck.TruckResponse;
import com.gayale.transport.exception.DuplicateResourceException;
import com.gayale.transport.exception.ResourceNotFoundException;
import com.gayale.transport.iservices.ITruckService;
import com.gayale.transport.model.Truck;
import com.gayale.transport.model.TransporterEnterprise;
import com.gayale.transport.repository.TruckRepository;
import com.gayale.transport.repository.TransporterEnterpriseRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TruckService implements ITruckService {
    private final TruckRepository truckRepository;
    private final TransporterEnterpriseRepository transporterEnterpriseRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public TruckService(TruckRepository truckRepository,
                        TransporterEnterpriseRepository transporterEnterpriseRepository,
                        ModelMapper modelMapper) {
        this.truckRepository = truckRepository;
        this.transporterEnterpriseRepository = transporterEnterpriseRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public List<TruckResponse> getAllTrucks() {
        return truckRepository.findAll().stream()
                              .map(truck -> modelMapper.map(truck, TruckResponse.class))
                              .collect(Collectors.toList());
    }

    @Override
    public TruckResponse getTruckById(String id) {
        Truck truck = truckRepository.findById(id)
                                     .orElseThrow(() -> new ResourceNotFoundException("Truck not found with id: " + id));
        return modelMapper.map(truck, TruckResponse.class);
    }

    @Override
    public TruckResponse getTruckByVehicle(String vehicle) {
        Truck truck = truckRepository.findByVehicle(vehicle)
                                     .orElseThrow(() -> new ResourceNotFoundException("Truck not found with vehicle: " + vehicle));
        return modelMapper.map(truck, TruckResponse.class);
    }

    @Override
    public List<TruckResponse> getTrucksByTransporterId(String transporterId) {
        return truckRepository.findByTransporterId(transporterId).stream()
                              .map(truck -> modelMapper.map(truck, TruckResponse.class))
                              .collect(Collectors.toList());
    }

    @Override
    public List<TruckResponse> getTrucksByTransporterName(String transporterName) {
        return truckRepository.findByTransporterName(transporterName).stream()
                              .map(truck -> modelMapper.map(truck, TruckResponse.class))
                              .collect(Collectors.toList());
    }

    @Override
    public TruckResponse createTruck(TruckRequest truckRequest) {
        if (truckRepository.existsByVehicle(truckRequest.getVehicle())) {
            throw new DuplicateResourceException("Truck with this vehicle number " + truckRequest.getVehicle() + " already exists");
        }

        // Validate that the transporter exists
        TransporterEnterprise transporter = transporterEnterpriseRepository.findById(truckRequest.getTransporterId())
                                                                           .orElseThrow(() -> new ResourceNotFoundException("Transporter not found with id: " + truckRequest.getTransporterId()));

        Truck truck = modelMapper.map(truckRequest, Truck.class);
        truck.setTransporter(transporter); // Set the actual transporter object
        truck.setCreatedAt(LocalDateTime.now());
        truck.setUpdatedAt(LocalDateTime.now());

        Truck savedTruck = truckRepository.save(truck);
        return modelMapper.map(savedTruck, TruckResponse.class);
    }

    @Override
    public TruckResponse updateTruck(String id, TruckRequest truckRequest) {
        Truck existingTruck = truckRepository.findById(id)
                                             .orElseThrow(() -> new ResourceNotFoundException("Truck not found with id: " + id));

        // Check for duplicate vehicle number (only if vehicle is being changed)
        if (!existingTruck.getVehicle().equals(truckRequest.getVehicle()) &&
                truckRepository.existsByVehicle(truckRequest.getVehicle())) {
            throw new DuplicateResourceException("Truck with this vehicle number " + truckRequest.getVehicle() + " already exists");
        }

        // Validate that the transporter exists (if being changed)
        if (!existingTruck.getTransporter().getId().equals(truckRequest.getTransporterId())) {
            TransporterEnterprise transporter = transporterEnterpriseRepository.findById(truckRequest.getTransporterId())
                                                                               .orElseThrow(() -> new ResourceNotFoundException("Transporter not found with id: " + truckRequest.getTransporterId()));
            existingTruck.setTransporter(transporter);
        }

        existingTruck.setVehicle(truckRequest.getVehicle());
        existingTruck.setDriverName(truckRequest.getDriverName());
        existingTruck.setPhone(truckRequest.getPhone());
        existingTruck.setUpdatedAt(LocalDateTime.now());

        Truck updatedTruck = truckRepository.save(existingTruck);
        return modelMapper.map(updatedTruck, TruckResponse.class);
    }

    @Override
    public void deleteTruck(String id) {
        if (!truckRepository.existsById(id)) {
            throw new ResourceNotFoundException("Truck not found with id: " + id);
        }
        truckRepository.deleteById(id);
    }

    @Override
    public boolean vehicleExists(String vehicle) {
        return truckRepository.existsByVehicle(vehicle);
    }
}