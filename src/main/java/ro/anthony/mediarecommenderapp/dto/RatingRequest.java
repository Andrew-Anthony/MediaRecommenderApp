package ro.anthony.mediarecommenderapp.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Getter
@Setter
public class RatingRequest {
    @Min(1)
    @Max(10)
    private Double score;
    private String review;
}
