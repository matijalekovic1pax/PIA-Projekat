package com.coworking.config;

import com.coworking.model.User;
import com.coworking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Create admin user if not exists
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@coworking.com");
            admin.setPassword(passwordEncoder.encode("Admin1234!"));
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setPhone("+381600000000");
            admin.setType("admin");
            admin.setStatus("ACTIVE");
            userRepository.save(admin);
            log.info("Admin user created. Username: admin, Password: Admin1234!");
        }
    }
}
