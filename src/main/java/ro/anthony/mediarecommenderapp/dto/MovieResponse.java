package ro.anthony.mediarecommenderapp.dto;

import lombok.Getter;
import lombok.Setter;
import ro.anthony.mediarecommenderapp.model.Genre;
import ro.anthony.mediarecommenderapp.model.Movie;

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
    private Double predictedRating;

    public static MovieResponse fromMovie(Movie movie) {
        MovieResponse movieResponse = new MovieResponse();
        movieResponse.setId(movie.getId());
        movieResponse.setTmdbId(movie.getTmdbId());
        movieResponse.setTitle(movie.getTitle());
        movieResponse.setOverview(movie.getOverview());
        movieResponse.setPosterPath(movie.getPosterPath());
        movieResponse.setReleaseDate(movie.getReleaseDate());
        movieResponse.setVoteAverage(movie.getVoteAverage());
        movieResponse.setGenres(movie.getGenres().stream().map(Genre::getName).toList());
        return movieResponse;
    }
}
