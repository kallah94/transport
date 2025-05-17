package com.gayale.transport.iservices;

import java.util.List;

import com.gayale.transport.dto.TruckDto;

public interface ITruckService {

    List<TruckDto> getAllTrucks();

    TruckDto getTruckById(String id);

    TruckDto getTruckByVehicle(String vehicle);

    List<TruckDto> getTrucksByTransporter(String transporter);

    TruckDto createTruck(TruckDto truckDto);

    TruckDto updateTruck(String id, TruckDto truckDto);

    void deleteTruck(String id);

    boolean vehicleExists(String vehicle);
}
