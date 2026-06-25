package com.gayale.transport.config;

import com.gayale.transport.model.User;
import com.gayale.transport.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MongoDbSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public MongoDbSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Seed des comptes de démonstration (alignés avec les identifiants du frontend).
        // Idempotent : ne crée chaque compte que s'il n'existe pas déjà.
        seedUser("admin", "admin@gayaletransport.com", "Administrateur", "admin", User.UserRole.ADMIN);
        seedUser("agent", "agent@gayaletransport.com", "Agent de saisie", "agent", User.UserRole.AGENT);
        seedUser("guest", "guest@gayaletransport.com", "Invité", "guest", User.UserRole.GUEST);
    }

    private void seedUser(String username, String email, String fullName, String rawPassword, User.UserRole role) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        System.out.println("Seed user créé : " + username + " (rôle " + role + ")");
    }
}