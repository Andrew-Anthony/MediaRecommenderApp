package ro.anthony.mediarecommenderapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.anthony.mediarecommenderapp.model.ChatFeedback;
import ro.anthony.mediarecommenderapp.model.FeedbackSignal;
import ro.anthony.mediarecommenderapp.model.Movie;
import ro.anthony.mediarecommenderapp.model.WatchlistStatus;
import ro.anthony.mediarecommenderapp.repository.ChatFeedbackRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatFeedbackService {

    private final ChatFeedbackRepository repo;
    private final MovieService movieService;
    private final TmdbService tmdbService;
    private final UserService userService;
    private final WatchlistService watchlistService;

    @Transactional
    public FeedbackSignal record(Long userId, Integer tmdbId, FeedbackSignal signal) {
        Movie movie = movieService.findByTmdbId(Long.valueOf(tmdbId))
                .orElseGet(() -> tmdbService.importMovie(tmdbId));

        Optional<ChatFeedback> existing = repo.findByUserIdAndMovieId(userId, movie.getId());

        if (existing.isPresent() && existing.get().getSignal() == signal) {
            repo.delete(existing.get());
            return null;
        }

        ChatFeedback feedback = existing.orElseGet(() -> {
            ChatFeedback fb = new ChatFeedback();
            fb.setUser(userService.getUserById(userId));
            fb.setMovie(movie);
            return fb;
        });
        feedback.setSignal(signal);
        repo.save(feedback);

        if (signal == FeedbackSignal.POSITIVE) {
            addToWatchlistIfAbsent(userId, movie.getId());
        }
        return signal;
    }

    public List<Integer> getDislikedTmdbIds(Long userId) {
        return repo.findByUserId(userId).stream()
                .filter(fb -> fb.getSignal() == FeedbackSignal.NEGATIVE)
                .map(fb -> fb.getMovie().getTmdbId().intValue())
                .toList();
    }

    public List<ChatFeedback> getByUser(Long userId) {
        return repo.findByUserId(userId);
    }

    @Transactional
    public void removeForMovie(Long userId, Long movieId) {
        repo.deleteByUserIdAndMovieId(userId, movieId);
    }

    public String formatForPrompt(Long userId, int limit) {
        List<ChatFeedback> all = repo.findByUserId(userId);
        if (all.isEmpty()) {
            return "";
        }
        all.sort(Comparator.comparing(ChatFeedback::getCreatedAt).reversed());
        List<ChatFeedback> slice = (limit > 0 && all.size() > limit) ? all.subList(0, limit) : all;

        String liked = slice.stream().filter(f -> f.getSignal() == FeedbackSignal.POSITIVE)
                .map(f -> f.getMovie().getTitle()).collect(Collectors.joining(", "));
        String disliked = slice.stream().filter(f -> f.getSignal() == FeedbackSignal.NEGATIVE)
                .map(f -> f.getMovie().getTitle()).collect(Collectors.joining(", "));

        StringBuilder sb = new StringBuilder();
        if (!liked.isBlank()) sb.append("👍 Liked: ").append(liked).append("\n");
        if (!disliked.isBlank()) sb.append("👎 Disliked: ").append(disliked).append("\n");
        return sb.toString();
    }

    private void addToWatchlistIfAbsent(Long userId, Long movieId) {
        boolean present = watchlistService.getUserWatchlist(userId).stream()
                .anyMatch(item -> item.getMovie().getId().equals(movieId));
        if (!present) {
            watchlistService.addToWatchlist(userId, movieId, WatchlistStatus.PLAN_TO_WATCH);
        }
    }
}
