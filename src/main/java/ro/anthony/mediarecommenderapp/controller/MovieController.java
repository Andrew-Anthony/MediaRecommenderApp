package ro.anthony.mediarecommenderapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.anthony.mediarecommenderapp.dto.MovieResponse;
import ro.anthony.mediarecommenderapp.model.Genre;
import ro.anthony.mediarecommenderapp.model.Movie;
import ro.anthony.mediarecommenderapp.service.MovieService;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {
    private final MovieService movieService;

    private MovieResponse toMovieResponse(Movie movie) {
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

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovieById(@PathVariable Long id) {
        Movie movie = movieService.getMovieById(id);
        return ResponseEntity.ok(toMovieResponse(movie));
    }

    @GetMapping("/search")
    public ResponseEntity<List<MovieResponse>> searchMovies(@RequestParam String keyword) {
        List<Movie> movieList = movieService.searchMovies(keyword);
        List<MovieResponse> movieResponseList = movieList.stream().map(this::toMovieResponse).toList();
        return ResponseEntity.ok(movieResponseList);
    }

    @GetMapping("/genres/{genreName}")
    public ResponseEntity<List<MovieResponse>> searchByGenres(@PathVariable String genreName) {
        List<Movie> movieList = movieService.getMoviesByGenre(genreName);
        List<MovieResponse> movieResponseList = movieList.stream().map(this::toMovieResponse).toList();
        return ResponseEntity.ok(movieResponseList);
    }
}
