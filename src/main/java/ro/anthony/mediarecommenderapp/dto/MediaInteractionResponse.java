package ro.anthony.mediarecommenderapp.dto;

import lombok.Getter;
import lombok.Setter;
import ro.anthony.mediarecommenderapp.model.InteractionType;

import java.time.LocalDateTime;

@Getter
@Setter
public class MediaInteractionResponse {
    private Long id;
    private Long userId;
    private String username;
    private Long movieId;
    private String movieTitle;
    private InteractionType interactionType;
    private LocalDateTime timestamp;
}
