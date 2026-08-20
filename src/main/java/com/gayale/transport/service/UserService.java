package com.gayale.transport.service;

import com.gayale.transport.dto.UserDto;
import com.gayale.transport.exception.ResourceNotFoundException;
import com.gayale.transport.model.User;
import com.gayale.transport.repository.TruckRepository;
import com.gayale.transport.repository.UserRepository;
import com.gayale.transport.license.LicenseGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TruckRepository truckRepository;
    private final LicenseGuard licenseGuard;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       TruckRepository truckRepository, LicenseGuard licenseGuard) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.truckRepository = truckRepository;
        this.licenseGuard = licenseGuard;
    }

    // Un compte chauffeur (DRIVER) doit être rattaché à un camion existant ; les autres rôles n'en ont pas.
    private String normalizeVehicleForRole(User.UserRole role, String vehicle) {
        if (role == User.UserRole.DRIVER) {
            if (vehicle == null || vehicle.isBlank()) {
                throw new IllegalArgumentException("Un compte chauffeur doit être rattaché à un camion (vehicle).");
            }
            if (!truckRepository.existsByVehicle(vehicle)) {
                throw new IllegalArgumentException("Camion introuvable : " + vehicle);
            }
            return vehicle;
        }
        return null;
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                             .map(this::mapUserToDto)
                             .collect(Collectors.toList());
    }

    public UserDto getUserById(String id) {
        User user = userRepository.findById(id)
                                  .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapUserToDto(user);
    }

    public UserDto createUser(UserDto userDto) {
        // Quota contractuel : refuse la creation au-dela du nombre d'utilisateurs vendu (HTTP 402).
        licenseGuard.checkUserQuota();

        // Check if username or email already exists
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setFullName(userDto.getFullName());
        user.setEmail(userDto.getEmail());
        user.setRole(userDto.getRole());
        user.setVehicle(normalizeVehicleForRole(userDto.getRole(), userDto.getVehicle()));

        User savedUser = userRepository.save(user);
        return mapUserToDto(savedUser);
    }

    public UserDto updateUser(String id, UserDto userDto) {
        User existingUser = userRepository.findById(id)
                                          .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Check if username is being changed and if it already exists
        if (!existingUser.getUsername().equals(userDto.getUsername()) &&
                userRepository.existsByUsername(userDto.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email is being changed and if it already exists
        if (!existingUser.getEmail().equals(userDto.getEmail()) &&
                userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        existingUser.setUsername(userDto.getUsername());
        existingUser.setFullName(userDto.getFullName());
        existingUser.setEmail(userDto.getEmail());
        existingUser.setRole(userDto.getRole());
        existingUser.setVehicle(normalizeVehicleForRole(userDto.getRole(), userDto.getVehicle()));

        // Only update password if provided
        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }

        User updatedUser = userRepository.save(existingUser);
        return mapUserToDto(updatedUser);
    }

    public boolean deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
        return true;
    }

    private UserDto mapUserToDto(User user) {
        return UserDto.builder()
                      .id(user.getId())
                      .username(user.getUsername())
                      .fullName(user.getFullName())
                      .email(user.getEmail())
                      .role(user.getRole())
                      .vehicle(user.getVehicle())
                      .lastLogin(user.getLastLogin())
                      .createdAt(user.getCreatedAt())
                      .build();
    }
}