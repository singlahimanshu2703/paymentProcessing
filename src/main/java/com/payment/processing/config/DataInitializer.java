package com.payment.processing.config;

import com.payment.processing.entity.User;
import com.payment.processing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            if (userRepository.findByUsername("admin").isEmpty()) {
                User user = new User();
                user.setUsername("admin");
                user.setPassword(passwordEncoder.encode("password"));
                userRepository.save(user);
                log.info("Created default user: admin/password");
            }
        } catch (Exception e) {
            // Ignore if database/tables not ready (e.g., in tests)
            log.debug("Could not initialize default user: {}", e.getMessage());
        }
    }
}
