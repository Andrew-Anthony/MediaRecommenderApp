package ro.anthony.mediarecommenderapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ro.anthony.mediarecommenderapp.exception.ResourceNotFoundException;
import ro.anthony.mediarecommenderapp.model.Genre;
import ro.anthony.mediarecommenderapp.repository.GenreRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenreService {
    private final GenreRepository genreRepository;

    public Genre getGenreByTmdbId(Integer tmdbId) {
        return genreRepository.findByTmdbId(tmdbId)
                .orElseThrow(() -> new ResourceNotFoundException("Genre not found with tmdb id: " + tmdbId));
    }

    public Genre getGenreByName(String name) {
        return genreRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Genre not found with name: " + name));
    }

    public List<Genre> getAllGenres() {
        return genreRepository.findAll();
    }

    public Genre saveGenre(Genre genre) {
        return genreRepository.findByTmdbId(genre.getTmdbId())
                .orElseGet(() -> genreRepository.save(genre));
    }
}
