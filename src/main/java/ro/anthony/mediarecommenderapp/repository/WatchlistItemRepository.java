package ro.anthony.mediarecommenderapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.anthony.mediarecommenderapp.model.WatchlistItem;
import ro.anthony.mediarecommenderapp.model.WatchlistStatus;
import java.util.List;
import java.util.Optional;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {
    List<WatchlistItem> findByUserId(Long userId);
    List<WatchlistItem> findByUserIdAndStatus(Long userId, WatchlistStatus status);
    Optional<WatchlistItem> findByUserIdAndMovieId(Long userId, Long movieId);
}
