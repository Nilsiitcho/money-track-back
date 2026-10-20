package com.moneytrack.security;

import com.moneytrack.common.ApiException;
import com.moneytrack.user.User;
import com.moneytrack.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwtService) {
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        User user = users.findByUsername(req.username())
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario ou senha invalidos"));
        String token = jwtService.generate(user.getId(), user.getUsername());
        return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds(), user.getUsername());
    }

    public record LoginRequest(
            @NotBlank(message = "Informe o usuario") String username,
            @NotBlank(message = "Informe a senha") String password) {
    }

    public record LoginResponse(String token, String tokenType, long expiresIn, String username) {
    }
}
