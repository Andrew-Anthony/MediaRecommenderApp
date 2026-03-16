package ro.anthony.mediarecommenderapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String email;
}
