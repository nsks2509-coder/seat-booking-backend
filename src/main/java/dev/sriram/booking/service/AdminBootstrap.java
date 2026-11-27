package dev.sriram.booking.service;

import dev.sriram.booking.domain.AppUser;
import dev.sriram.booking.repository.AppUserRepository;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminBootstrap(AppUserRepository users, PasswordEncoder passwordEncoder,
                          @Value("${app.bootstrap.admin.username:}") String username,
                          @Value("${app.bootstrap.admin.password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() && password.isBlank()) return;
        if (username.isBlank() || password.length() < 12 || password.length() > 72) {
            throw new IllegalStateException("Set both ADMIN_USERNAME and ADMIN_PASSWORD; password length must be 12-72 characters");
        }
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        users.findByUsername(normalized).ifPresentOrElse(existing -> {
            if (!"ADMIN".equals(existing.getRole())) {
                throw new IllegalStateException("Configured bootstrap account already exists without ADMIN role");
            }
        }, () -> users.save(new AppUser(normalized, passwordEncoder.encode(password), "ADMIN")));
    }
}
