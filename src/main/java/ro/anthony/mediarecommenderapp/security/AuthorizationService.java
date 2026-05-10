package ro.anthony.mediarecommenderapp.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ro.anthony.mediarecommenderapp.exception.ForbiddenException;
import ro.anthony.mediarecommenderapp.model.User;
import ro.anthony.mediarecommenderapp.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class AuthorizationService {
    private final UserRepository userRepository;

    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new SecurityException("Not authenticated");
        }
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new SecurityException("Authenticated user not found"));
        return user.getId();
    }

    public void verifyOwnership(Long pathUserId) {
        Long currentUserId = getCurrentUserId();
        if (!currentUserId.equals(pathUserId)) {
            throw new ForbiddenException("You are not authorized to access this resource");
        }
    }
}
