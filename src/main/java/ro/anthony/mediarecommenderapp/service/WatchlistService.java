package ro.anthony.mediarecommenderapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ro.anthony.mediarecommenderapp.exception.ResourceNotFoundException;
import ro.anthony.mediarecommenderapp.model.Movie;
import ro.anthony.mediarecommenderapp.model.User;
import ro.anthony.mediarecommenderapp.model.WatchlistItem;
import ro.anthony.mediarecommenderapp.model.WatchlistStatus;
import ro.anthony.mediarecommenderapp.repository.WatchlistItemRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchlistService {
    private final WatchlistItemRepository watchlistItemRepository;
    private final UserService userService;
    private final MovieService movieService;

    public List<WatchlistItem> getUserWatchlist(Long userId) {
        return watchlistItemRepository.findByUserId(userId);
    }

    public List<WatchlistItem> getUserWatchlistByStatus(Long userId, WatchlistStatus status) {
        return watchlistItemRepository.findByUserIdAndStatus(userId, status);
    }

    public WatchlistItem addToWatchlist(Long userId, Long movieId, WatchlistStatus status) {
        User user = userService.getUserById(userId);
        Movie movie = movieService.getMovieById(movieId);

        if(watchlistItemRepository.findByUserIdAndMovieId(userId, movieId).isPresent()) {
            throw new IllegalArgumentException("Movie already in watchlist");
        }

        WatchlistItem watchlistItem = new WatchlistItem();
        watchlistItem.setUser(user);
        watchlistItem.setMovie(movie);
        watchlistItem.setStatus(status);

        return watchlistItemRepository.save(watchlistItem);
    }

    public WatchlistItem updateWatchlistStatus(Long userId, Long movieId, WatchlistStatus status) {
        WatchlistItem watchlistItem = watchlistItemRepository.findByUserIdAndMovieId(userId, movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found in watchlist"));
        watchlistItem.setStatus(status);

        return watchlistItemRepository.save(watchlistItem);
    }

    public void removeFromWatchlist(Long userId, Long movieId) {
        WatchlistItem watchlistItem = watchlistItemRepository.findByUserIdAndMovieId(userId, movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found in watchlist"));
        watchlistItemRepository.delete(watchlistItem);
    }

    public void markAsWatched(Long userId, Long movieId) {
        watchlistItemRepository.findByUserIdAndMovieId(userId, movieId)
                .ifPresentOrElse(
                        item -> {
                            item.setStatus(WatchlistStatus.WATCHED);
                            watchlistItemRepository.save(item);
                        },
                        () -> {
                            User user = userService.getUserById(userId);
                            Movie movie = movieService.getMovieById(movieId);
                            WatchlistItem item = new WatchlistItem();
                            item.setUser(user);
                            item.setMovie(movie);
                            item.setStatus(WatchlistStatus.WATCHED);
                            watchlistItemRepository.save(item);
                        }
                );
    }
}
