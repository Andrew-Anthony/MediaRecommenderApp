package ro.anthony.mediarecommenderapp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.anthony.mediarecommenderapp.dto.RatingRequest;
import ro.anthony.mediarecommenderapp.dto.RatingResponse;
import ro.anthony.mediarecommenderapp.model.Rating;
import ro.anthony.mediarecommenderapp.service.RatingService;
import ro.anthony.mediarecommenderapp.security.AuthorizationService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RatingController {
    private final RatingService ratingService;
    private final AuthorizationService authorizationService;

    private RatingResponse toRatingResponse(Rating rating) {
        RatingResponse ratingResponse = new RatingResponse();
        ratingResponse.setId(rating.getId());
        ratingResponse.setUserId(rating.getUser().getId());
        ratingResponse.setUsername(rating.getUser().getUsername());
        ratingResponse.setMovieId(rating.getMovie().getId());
        ratingResponse.setMovieTitle(rating.getMovie().getTitle());
        ratingResponse.setScore(rating.getScore());
        ratingResponse.setReview(rating.getReview());
        ratingResponse.setCreatedAt(rating.getCreatedAt());
        return ratingResponse;
    }

    @PostMapping("/users/{userId}/movies/{movieId}/ratings")
    public ResponseEntity<RatingResponse> addRating(@PathVariable Long userId, @PathVariable Long movieId,
                                                    @Valid @RequestBody RatingRequest request) {
        authorizationService.verifyOwnership(userId);
        Rating rating = ratingService.addRating(
                userId,
                movieId,
                request.getScore(),
                request.getReview()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toRatingResponse(rating));
    }

    @PutMapping("/users/{userId}/movies/{movieId}/ratings")
    public ResponseEntity<RatingResponse> updateRating(@PathVariable Long userId, @PathVariable Long movieId,
                                                       @Valid @RequestBody RatingRequest request) {
        authorizationService.verifyOwnership(userId);
        Rating rating = ratingService.updateRating(
                userId,
                movieId,
                request.getScore(),
                request.getReview()
        );

        return ResponseEntity.ok(toRatingResponse(rating));
    }

    @DeleteMapping("/users/{userId}/movies/{movieId}/ratings")
    public ResponseEntity<Void> deleteRating(@PathVariable Long userId, @PathVariable Long movieId) {
        authorizationService.verifyOwnership(userId);
        ratingService.deleteRating(userId, movieId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}/ratings")
    public ResponseEntity<List<RatingResponse>> getUserRatings(@PathVariable Long userId) {
        authorizationService.verifyOwnership(userId);
        List<Rating> ratingList = ratingService.getRatingsByUser(userId);
        List<RatingResponse> ratingResponseList = ratingList.stream().map(this::toRatingResponse).toList();
        return ResponseEntity.ok(ratingResponseList);
    }

    @GetMapping("/movies/{movieId}/ratings")
    public ResponseEntity<List<RatingResponse>> getMovieRatings(@PathVariable Long movieId) {
        List<Rating> ratingList = ratingService.getRatingsByMovie(movieId);
        List<RatingResponse> ratingResponseList = ratingList.stream().map(this::toRatingResponse).toList();
        return ResponseEntity.ok(ratingResponseList);
    }

    @GetMapping("/users/{userId}/movies/{movieId}/ratings")
    public ResponseEntity<RatingResponse>  getUserRatingForMovie(@PathVariable Long userId, @PathVariable Long movieId) {
        Rating rating = ratingService.getUserRatingForMovie(userId, movieId);
        return ResponseEntity.ok(toRatingResponse(rating));
    }
}
