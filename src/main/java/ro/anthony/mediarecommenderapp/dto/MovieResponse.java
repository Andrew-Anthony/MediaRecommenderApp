package ro.anthony.mediarecommenderapp.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class MovieResponse {
    private Long id;
    private Long tmdbId;
    private String title;
    private String overview;
    private String posterPath;
    private LocalDate releaseDate;
    private Double voteAverage;
    private List<String> genres;
}
