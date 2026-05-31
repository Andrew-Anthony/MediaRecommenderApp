package ro.anthony.mediarecommenderapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ro.anthony.mediarecommenderapp.exception.ResourceNotFoundException;
import ro.anthony.mediarecommenderapp.model.Movie;
import ro.anthony.mediarecommenderapp.model.Rating;
import ro.anthony.mediarecommenderapp.model.User;
import ro.anthony.mediarecommenderapp.repository.RatingRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratingRepository;
    private final UserService userService;
    private final MovieService movieService;
    private final WatchlistService watchlistService;
    private final ChatFeedbackService chatFeedbackService;

    public List<Rating> getRatingsByUser(Long userId) {
        return ratingRepository.findByUserId(userId);
    }

    public List<Rating> getRatingsByMovie(Long movieId) {
        return ratingRepository.findByMovieId(movieId);
    }

    public Rating getUserRatingForMovie(Long userId, Long movieId) {
        return ratingRepository.findByUserIdAndMovieId(userId, movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot find rating for movie with id: " + movieId + " from user with id: " + userId));
    }

    public Rating addRating(Long userId, Long movieId, Double score, String review) {
        User user = userService.getUserById(userId);
        Movie movie = movieService.getMovieById(movieId);

        if(ratingRepository.findByUserIdAndMovieId(userId, movieId).isPresent()) {
            throw new IllegalArgumentException("User already rated this movie");
        }

        Rating rating = new Rating();
        rating.setUser(user);
        rating.setMovie(movie);
        rating.setScore(score);
        rating.setReview(review);

        Rating saved = ratingRepository.save(rating);
        watchlistService.markAsWatched(userId, movieId);
        chatFeedbackService.removeForMovie(userId, movieId);
        return saved;
    }

    public Rating updateRating(Long userId, Long movieId, Double score, String review) {
        Rating rating = getUserRatingForMovie(userId, movieId);

        rating.setScore(score);
        rating.setReview(review);

        return ratingRepository.save(rating);
    }

    public void deleteRating(Long userId, Long movieId) {
        Rating rating = getUserRatingForMovie(userId, movieId);
        ratingRepository.delete(rating);
    }
}
