package com.gayale.transport.controller;

import java.security.KeyStore;
import java.util.List;

import com.gayale.transport.dto.TruckDto;
import com.gayale.transport.service.TruckService;
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
@RequestMapping("trucks")
@SecurityRequirement(name = "JWT")
@Tag(name = "Trucks", description = "API for Truck Management")
public class TruckController {

    private final TruckService truckService;

    @Autowired
    public TruckController(TruckService truckService) {
        this.truckService = truckService;
    }

    @GetMapping
    public ResponseEntity<List<TruckDto>> getAllTrucks() {
        return ResponseEntity.ok(truckService.getAllTrucks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TruckDto> getTruckById(@PathVariable String id) {
        return ResponseEntity.ok(truckService.getTruckById(id));
    }

    @GetMapping("/vehicle/{vehicle}")
    public ResponseEntity<TruckDto> getTruckByVehicle(@PathVariable String vehicle) {
        return ResponseEntity.ok(truckService.getTruckByVehicle(vehicle));
    }

    @GetMapping("/transporter/{transporter}")
    public ResponseEntity<List<TruckDto>> getTrucksByTransporter(@PathVariable String transporter) {
        return ResponseEntity.ok(truckService.getTrucksByTransporter(transporter));
    }

    @PostMapping
    public ResponseEntity<TruckDto> createTruck(@Valid @RequestBody TruckDto truckDto) {
        return new ResponseEntity<>(truckService.createTruck(truckDto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TruckDto> updateTruck(@PathVariable String id, @Valid @RequestBody TruckDto truckDto) {
        return ResponseEntity.ok(truckService.updateTruck(id, truckDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTruck(@PathVariable String id) {
        truckService.deleteTruck(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check-vehicle/{vehicle}")
    public ResponseEntity<Boolean> checkVehicleExists(@PathVariable String vehicle) {
        return ResponseEntity.ok(truckService.vehicleExists(vehicle));
    }

}
