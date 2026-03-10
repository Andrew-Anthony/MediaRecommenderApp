package ro.anthony.mediarecommenderapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.anthony.mediarecommenderapp.model.Rating;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByUserId(Long userId);
    List<Rating> findByMovieId(Long movieId);
    Optional<Rating> findByUserIdAndMovieId(Long userId, Long movieId);
    List<Rating> findByScoreGreaterThanEqual(Double score);
    List<Rating> findByScoreLessThanEqual(Double score);
    List<Rating> findByScoreBetween(Double min, Double max);
}
