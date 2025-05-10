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
        // Check if there are any users in the database
        if (userRepository.count() == 0) {
            // Create admin user
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setEmail("admin@gayaletransport.com");
            adminUser.setFullName("Admin User");
            adminUser.setPassword(passwordEncoder.encode("Admin123!"));
            adminUser.setRole(User.UserRole.ADMIN);
            adminUser.setLastLogin(LocalDateTime.now());

            userRepository.save(adminUser);

            System.out.println("Admin user created successfully with username: admin and password: Admin123!");
        }
    }
}