package ro.anthony.mediarecommenderapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ro.anthony.mediarecommenderapp.dto.MovieResponse;
import ro.anthony.mediarecommenderapp.dto.RecommendationResponse;
import ro.anthony.mediarecommenderapp.model.Movie;
import ro.anthony.mediarecommenderapp.model.Rating;
import ro.anthony.mediarecommenderapp.model.WatchlistItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final RestClient recommendationRestClient;
    private final MovieService movieService;
    private final TmdbService tmdbService;
    private final RatingService ratingService;
    private final WatchlistService watchlistService;

    public List<MovieResponse> getRecommendations(Long userId, int n) {
        List<Rating> userRatings = ratingService.getRatingsByUser(userId);

        List<Map<String, Object>> ratingsPayload = new ArrayList<>();
        for (Rating r : userRatings) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("tmdbId", r.getMovie().getTmdbId().intValue());
            entry.put("score", r.getScore());
            ratingsPayload.add(entry);
        }

        List<WatchlistItem> watchlistItems = watchlistService.getUserWatchlist(userId);
        List<Integer> excludeTmdbIds = new ArrayList<>();
        for (WatchlistItem item : watchlistItems) {
            excludeTmdbIds.add(item.getMovie().getTmdbId().intValue());
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("ratings", ratingsPayload);
        requestBody.put("excludeTmdbIds", excludeTmdbIds);
        requestBody.put("n", n);

        List<RecommendationResponse> recommendations = recommendationRestClient.post()
                .uri("/recommend")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(new ParameterizedTypeReference<List<RecommendationResponse>>() {
                });

        if (recommendations == null || recommendations.isEmpty()) {
            return new ArrayList<>();
        }

        List<MovieResponse> movieResponses = new ArrayList<>();

        for (RecommendationResponse rec : recommendations) {
            if (rec.getTmdbId() == null) {
                continue;
            }

            Movie movie = movieService.findByTmdbId(Long.valueOf(rec.getTmdbId()))
                    .orElseGet(() -> tmdbService.importMovie(rec.getTmdbId()));

            MovieResponse response = MovieResponse.fromMovie(movie);
            response.setPredictedRating(rec.getPredictedRating());
            movieResponses.add(response);
        }

        return movieResponses;
    }
}
