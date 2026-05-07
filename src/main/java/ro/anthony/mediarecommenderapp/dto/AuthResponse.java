package ro.anthony.mediarecommenderapp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {
    private Long userId;
    private String token;
    private String username;
}
