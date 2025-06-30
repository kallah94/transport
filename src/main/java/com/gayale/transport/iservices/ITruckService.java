package com.gayale.transport.iservices;

import java.util.List;

import com.gayale.transport.dto.truck.TruckRequest;
import com.gayale.transport.dto.truck.TruckResponse;

public interface ITruckService {

    List<TruckResponse> getAllTrucks();

    TruckResponse getTruckById(String id);

    TruckResponse getTruckByVehicle(String vehicle);

    // Find trucks by transporter ID (more practical)
    List<TruckResponse> getTrucksByTransporterId(String transporterId);

    // Find trucks by transporter name (for flexibility)
    List<TruckResponse> getTrucksByTransporterName(String transporterName);

    TruckResponse createTruck(TruckRequest truckRequest);

    TruckResponse updateTruck(String id, TruckRequest truckRequest);

    void deleteTruck(String id);

    boolean vehicleExists(String vehicle);
}