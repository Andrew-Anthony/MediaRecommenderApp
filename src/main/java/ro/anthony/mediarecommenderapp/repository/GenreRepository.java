package ro.anthony.mediarecommenderapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.anthony.mediarecommenderapp.model.Genre;

import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genre, Long> {
    Optional<Genre> findByTmdbId(Integer tmdbId);
    Optional<Genre> findByName(String name);
}
