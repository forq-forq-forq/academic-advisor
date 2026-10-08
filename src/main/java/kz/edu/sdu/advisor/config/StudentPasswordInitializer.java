package kz.edu.sdu.advisor.config;

import kz.edu.sdu.advisor.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Sets a default test password for students that do not have one yet.
 * TODO: replace with real password provisioning.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class StudentPasswordInitializer implements CommandLineRunner {

    private static final String DEFAULT_PASSWORD = "password123";

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        studentRepository.findAll().stream()
            .filter(s -> s.getPasswordHash() == null)
            .forEach(s -> {
                s.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
                studentRepository.save(s);
            });
    }
}
