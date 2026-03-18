package ro.anthony.mediarecommenderapp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import ro.anthony.mediarecommenderapp.model.InteractionType;

@Getter
@Setter
public class MediaInteractionRequest {
    @NotNull
    private Long movieId;

    @NotNull
    private InteractionType interactionType;
}
