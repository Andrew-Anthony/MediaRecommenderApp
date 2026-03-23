package ro.anthony.mediarecommenderapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.anthony.mediarecommenderapp.dto.MovieResponse;
import ro.anthony.mediarecommenderapp.model.Movie;
import ro.anthony.mediarecommenderapp.service.MovieService;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {
    private final MovieService movieService;

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovieById(@PathVariable Long id) {
        Movie movie = movieService.getMovieById(id);
        return ResponseEntity.ok(MovieResponse.fromMovie(movie));
    }

    @GetMapping("/search")
    public ResponseEntity<List<MovieResponse>> searchMovies(@RequestParam String keyword) {
        List<Movie> movieList = movieService.searchMovies(keyword);
        List<MovieResponse> movieResponseList = movieList.stream().map(MovieResponse::fromMovie).toList();
        return ResponseEntity.ok(movieResponseList);
    }

    @GetMapping("/genres/{genreName}")
    public ResponseEntity<List<MovieResponse>> searchByGenres(@PathVariable String genreName) {
        List<Movie> movieList = movieService.getMoviesByGenre(genreName);
        List<MovieResponse> movieResponseList = movieList.stream().map(MovieResponse::fromMovie).toList();
        return ResponseEntity.ok(movieResponseList);
    }
}

