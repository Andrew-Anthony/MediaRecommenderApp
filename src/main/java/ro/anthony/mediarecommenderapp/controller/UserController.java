package ro.anthony.mediarecommenderapp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.anthony.mediarecommenderapp.dto.ChangePasswordRequest;
import ro.anthony.mediarecommenderapp.dto.UserRegistrationRequest;
import ro.anthony.mediarecommenderapp.dto.UserResponse;
import ro.anthony.mediarecommenderapp.dto.UserUpdateRequest;
import ro.anthony.mediarecommenderapp.model.User;
import ro.anthony.mediarecommenderapp.service.UserService;
import ro.anthony.mediarecommenderapp.security.AuthorizationService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AuthorizationService authorizationService;

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        authorizationService.verifyOwnership(id);
        User user = userService.getUserById(id);

        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setUsername(user.getUsername());
        userResponse.setEmail(user.getEmail());
        userResponse.setCreatedAt(user.getCreatedAt());

        return ResponseEntity.ok(userResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        User user = userService.registerUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword()
        );

        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setUsername(user.getUsername());
        userResponse.setEmail(user.getEmail());
        userResponse.setCreatedAt(user.getCreatedAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        authorizationService.verifyOwnership(id);
        User user = userService.updateUser(
                id,
                request.getUsername(),
                request.getEmail()
        );

        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setUsername(user.getUsername());
        userResponse.setEmail(user.getEmail());
        userResponse.setCreatedAt(user.getCreatedAt());

        return ResponseEntity.ok(userResponse);
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(@PathVariable Long id,
                                               @RequestBody ChangePasswordRequest request) {
        authorizationService.verifyOwnership(id);
        userService.changePassword(id, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        authorizationService.verifyOwnership(id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
