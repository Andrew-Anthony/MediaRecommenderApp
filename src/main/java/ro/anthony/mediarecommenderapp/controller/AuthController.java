package ro.anthony.mediarecommenderapp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.anthony.mediarecommenderapp.dto.AuthResponse;
import ro.anthony.mediarecommenderapp.dto.LoginRequest;
import ro.anthony.mediarecommenderapp.dto.UserRegistrationRequest;
import ro.anthony.mediarecommenderapp.security.JwtService;
import ro.anthony.mediarecommenderapp.service.UserService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserRegistrationRequest request) {
        userService.registerUser(request.getUsername(), request.getEmail(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        String token = jwtService.generateToken(request.getUsername());

        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken(token);
        authResponse.setUsername(request.getUsername());

        return ResponseEntity.ok(authResponse);
    }
}
