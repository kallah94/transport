package com.gayale.transport.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.gayale.transport.dto.TruckDto;
import com.gayale.transport.exception.DuplicateResourceException;
import com.gayale.transport.exception.ResourceNotFoundException;
import com.gayale.transport.iservices.ITruckService;
import com.gayale.transport.model.Truck;
import com.gayale.transport.repository.TruckRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TruckService implements ITruckService {
    private final TruckRepository truckRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public TruckService(TruckRepository truckRepository, ModelMapper modelMapper) {
        this.truckRepository = truckRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public List<TruckDto> getAllTrucks() {
        return truckRepository.findAll().stream()
                .map(truck -> modelMapper.map(truck, TruckDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public TruckDto getTruckById(String id) {
        Truck truck = truckRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Truck not found with id: " + id));
        return modelMapper.map(truck, TruckDto.class);
    }

    @Override
    public TruckDto getTruckByVehicle(String vehicle) {
        Truck truck = truckRepository.findByVehicle(vehicle)
                .orElseThrow(() -> new ResourceNotFoundException("Truck not found with vehicle: "+ vehicle));
        return modelMapper.map(truck, TruckDto.class);
    }

    @Override
    public List<TruckDto> getTrucksByTransporter(String transporter) {
        return truckRepository.findByTransporter(transporter).stream()
                .map(truck -> modelMapper.map(truck, TruckDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public TruckDto createTruck(TruckDto truckDto) {
        if (truckRepository.existsByVehicle(truckDto.getVehicle())) {
            throw new DuplicateResourceException("Truck with this vehicle number " + truckDto.getVehicle() + "already exists");
        }

        Truck truck = modelMapper.map(truckDto, Truck.class);
        truck.setCreatedAt(LocalDateTime.now());
        truck.setUpdatedAt(LocalDateTime.now());

        Truck savedTruck = truckRepository.save(truck);
        return modelMapper.map(savedTruck, TruckDto.class);
    }

    @Override
    public TruckDto updateTruck(String id, TruckDto truckDto) {
        Truck existingTruck = truckRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Truck not found with id: " + id));

        if (!existingTruck.getVehicle().equals(truckDto.getVehicle()) &&
            truckRepository.existsByVehicle(truckDto.getVehicle())) {
            throw new DuplicateResourceException("Truck with this vehicle number "+ truckDto.getUpdatedAt() + "already exists");
        }

        existingTruck.setVehicle(truckDto.getVehicle());
        existingTruck.setTransporter(truckDto.getTransporter());
        existingTruck.setDriverName(truckDto.getDriverName());
        existingTruck.setPhone(truckDto.getPhone());
        existingTruck.setUpdatedAt(LocalDateTime.now());

        Truck updatedTruck = truckRepository.save(existingTruck);
        return modelMapper.map(updatedTruck, TruckDto.class);
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
