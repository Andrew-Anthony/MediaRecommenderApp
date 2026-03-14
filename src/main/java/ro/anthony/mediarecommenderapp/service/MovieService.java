package ro.anthony.mediarecommenderapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ro.anthony.mediarecommenderapp.exception.ResourceNotFoundException;
import ro.anthony.mediarecommenderapp.model.Movie;
import ro.anthony.mediarecommenderapp.repository.MovieRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieService {
    private final MovieRepository movieRepository;

    public Movie getMovieById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot find movie with id:" + id));
    }

    public Movie getMovieByTmdbId(Long tmdbId) {
        return movieRepository.findByTmdbId(tmdbId)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot find movie with tmdb id:" + tmdbId));
    }

    public List<Movie> getMoviesByGenre(String genreName) {
        return movieRepository.findByGenres_Name(genreName);
    }

    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    public Movie saveMovie(Movie movie) {
        return movieRepository.findByTmdbId(movie.getTmdbId())
                .orElseGet(() -> movieRepository.save(movie));
    }

    public List<Movie> searchMovies(String keyword) {
        return movieRepository.findByTitleContainingIgnoreCase(keyword);
    }
}
