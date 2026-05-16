package ro.anthony.mediarecommenderapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.anthony.mediarecommenderapp.dto.MovieResponse;
import ro.anthony.mediarecommenderapp.dto.tmdb.TmdbSearchResponse;
import ro.anthony.mediarecommenderapp.model.Movie;
import ro.anthony.mediarecommenderapp.service.TmdbService;

@RestController
@RequestMapping("/api/tmdb")
@RequiredArgsConstructor
public class TmdbController {
    private final TmdbService tmdbService;

    @GetMapping("/search")
    public ResponseEntity<TmdbSearchResponse> searchTmdb(@RequestParam String query) {
        TmdbSearchResponse response = tmdbService.searchMovies(query);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/import/{tmdbId}")
    public ResponseEntity<MovieResponse> importMovie(@PathVariable int tmdbId) {
        Movie movie = tmdbService.importMovie(tmdbId);
        return ResponseEntity.status(HttpStatus.CREATED).body(MovieResponse.fromMovie(movie));
    }

    @PostMapping("/genres/sync")
    public ResponseEntity<Void> syncGenres() {
        tmdbService.syncGenres();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/import/top-rated")
    public ResponseEntity<String> importTopRated(@RequestParam(defaultValue = "5") int pages) {
        int imported = tmdbService.importTopRated(pages);
        return ResponseEntity.ok("Imported " + imported + " movies");
    }

    @PostMapping("/refresh-all")
    public ResponseEntity<String> refreshAll() {
        int refreshed = tmdbService.refreshAllMovies();
        return ResponseEntity.ok("Refreshed " + refreshed + " movies");
    }
}

