package ro.anthony.mediarecommenderapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ro.anthony.mediarecommenderapp.exception.ResourceNotFoundException;
import ro.anthony.mediarecommenderapp.model.User;
import ro.anthony.mediarecommenderapp.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    public User registerUser(String username, String email, String password) {
        if(userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken: " + username);
        }
        if(userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already taken: " + email);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));

        return userRepository.save(user);
    }

    public User updateUser(Long id, String username, String email){
        User user = getUserById(id);
        userRepository.findByUsername(username).ifPresent(existingUser -> {
            if(!existingUser.getId().equals(id)) {
                throw new IllegalArgumentException("Username already taken: " + username);
            }
        });

        userRepository.findByEmail(email).ifPresent(existingUser -> {
            if(!existingUser.getId().equals(id)) {
                throw new IllegalArgumentException("Email already taken: " + email);
            }
        });

        user.setUsername(username);
        user.setEmail(email);

        return userRepository.save(user);
    }

    public void deleteUser(Long id){
        User user = getUserById(id);
        userRepository.delete(user);
    }
}
