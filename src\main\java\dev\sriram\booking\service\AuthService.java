package dev.sriram.booking.service;

import dev.sriram.booking.domain.AppUser;
import dev.sriram.booking.repository.AppUserRepository;
import dev.sriram.booking.security.JwtService;
import dev.sriram.booking.web.dto.AuthRequest;
import dev.sriram.booking.web.dto.AuthResponse;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {
        String username = normalize(request.username());
        if (users.existsByUsername(username)) throw new ConflictException("Username is already registered");
        AppUser user = users.saveAndFlush(new AppUser(username, passwordEncoder.encode(request.password()), "USER"));
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        String username = normalize(request.username());
        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        return response(user);
    }

    private AuthResponse response(AppUser user) {
        var issued = jwtService.issue(user.getUsername(), user.getRole());
        return new AuthResponse(issued.token(), "Bearer", issued.expiresAt(), issued.role());
    }

    private String normalize(String username) { return username.trim().toLowerCase(Locale.ROOT); }
}
