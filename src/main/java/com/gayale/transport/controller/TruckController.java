package com.gayale.transport.controller;

import java.util.List;

import com.gayale.transport.dto.truck.TruckRequest;
import com.gayale.transport.dto.truck.TruckResponse;
import com.gayale.transport.dto.truck.TruckStatistics;
import com.gayale.transport.service.TruckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trucks")
@SecurityRequirement(name = "JWT")
@Tag(name = "Trucks", description = "API for Truck Management")
public class TruckController {

    private final TruckService truckService;

    @Autowired
    public TruckController(TruckService truckService) {
        this.truckService = truckService;
    }

    @GetMapping
    @Operation(summary = "Get all trucks", description = "Retrieve a list of all trucks")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved all trucks")
    public ResponseEntity<List<TruckResponse>> getAllTrucks() {
        return ResponseEntity.ok(truckService.getAllTrucks());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get truck by ID", description = "Retrieve a truck by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Truck found successfully"),
            @ApiResponse(responseCode = "404", description = "Truck not found")
    })
    public ResponseEntity<TruckResponse> getTruckById(
            @Parameter(description = "Truck ID") @PathVariable String id) {
        return ResponseEntity.ok(truckService.getTruckById(id));
    }

    @GetMapping("/{id}/statistics")
    @Operation(summary = "Truck statistics", description = "Tonnage, voyages et repartition pour un camion")
    public ResponseEntity<TruckStatistics> getTruckStatistics(@PathVariable String id) {
        return ResponseEntity.ok(truckService.getTruckStatistics(id));
    }

    @GetMapping("/vehicle/{vehicle}")
    @Operation(summary = "Get truck by vehicle number", description = "Retrieve a truck by its vehicle number")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Truck found successfully"),
            @ApiResponse(responseCode = "404", description = "Truck not found")
    })
    public ResponseEntity<TruckResponse> getTruckByVehicle(
            @Parameter(description = "Vehicle number") @PathVariable String vehicle) {
        return ResponseEntity.ok(truckService.getTruckByVehicle(vehicle));
    }

    @GetMapping("/transporter/{transporterId}")
    @Operation(summary = "Get trucks by transporter ID", description = "Retrieve all trucks belonging to a specific transporter")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved trucks for transporter")
    public ResponseEntity<List<TruckResponse>> getTrucksByTransporterId(
            @Parameter(description = "Transporter ID") @PathVariable String transporterId) {
        return ResponseEntity.ok(truckService.getTrucksByTransporterId(transporterId));
    }

    @GetMapping("/transporter/name/{transporterName}")
    @Operation(summary = "Get trucks by transporter name", description = "Retrieve all trucks belonging to a transporter by name")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved trucks for transporter")
    public ResponseEntity<List<TruckResponse>> getTrucksByTransporterName(
            @Parameter(description = "Transporter name") @PathVariable String transporterName) {
        return ResponseEntity.ok(truckService.getTrucksByTransporterName(transporterName));
    }

    @PostMapping
    @Operation(summary = "Create a new truck", description = "Create a new truck")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Truck created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid truck data"),
            @ApiResponse(responseCode = "409", description = "Truck with this vehicle number already exists")
    })
    public ResponseEntity<TruckResponse> createTruck(
            @Parameter(description = "Truck data") @Valid @RequestBody TruckRequest truckRequest) {
        return new ResponseEntity<>(truckService.createTruck(truckRequest), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a truck", description = "Update an existing truck")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Truck updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid truck data"),
            @ApiResponse(responseCode = "404", description = "Truck not found"),
            @ApiResponse(responseCode = "409", description = "Truck with this vehicle number already exists")
    })
    public ResponseEntity<TruckResponse> updateTruck(
            @Parameter(description = "Truck ID") @PathVariable String id,
            @Parameter(description = "Updated truck data") @Valid @RequestBody TruckRequest truckRequest) {
        return ResponseEntity.ok(truckService.updateTruck(id, truckRequest));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a truck", description = "Delete a truck by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Truck deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Truck not found")
    })
    public ResponseEntity<Void> deleteTruck(
            @Parameter(description = "Truck ID") @PathVariable String id) {
        truckService.deleteTruck(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check-vehicle/{vehicle}")
    @Operation(summary = "Check if vehicle exists", description = "Check if a truck with the given vehicle number exists")
    @ApiResponse(responseCode = "200", description = "Vehicle existence check completed")
    public ResponseEntity<Boolean> checkVehicleExists(
            @Parameter(description = "Vehicle number") @PathVariable String vehicle) {
        return ResponseEntity.ok(truckService.vehicleExists(vehicle));
    }
}