package ro.anthony.mediarecommenderapp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecommendationResponse {
    private Integer movieId;
    private Double predictedRating;
    private Integer tmdbId;
}
