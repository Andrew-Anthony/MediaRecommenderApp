package ro.anthony.mediarecommenderapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ro.anthony.mediarecommenderapp.dto.MovieResponse;
import ro.anthony.mediarecommenderapp.dto.RecommendationResponse;
import ro.anthony.mediarecommenderapp.dto.chat.LLMIntentFilters;
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
        return getCandidates(userId, n, true);
    }

    public List<MovieResponse> getCandidates(Long userId, int n, boolean applyQualityFilter) {
        return getCandidates(userId, n, applyQualityFilter, null);
    }

    public List<MovieResponse> getCandidates(Long userId, int n, boolean applyQualityFilter, LLMIntentFilters filters) {
        List<Rating> userRatings = ratingService.getRatingsByUser(userId);

        if (userRatings.isEmpty()) {
            return new ArrayList<>();
        }

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
        requestBody.put("applyQualityFilter", applyQualityFilter);

        if (filters != null) {
            Map<String, Object> filtersPayload = new HashMap<>();
            if (filters.getGenres() != null && !filters.getGenres().isEmpty()) {
                filtersPayload.put("genres", filters.getGenres());
            }
            if (filters.getMinYear() != null) filtersPayload.put("minYear", filters.getMinYear());
            if (filters.getMaxYear() != null) filtersPayload.put("maxYear", filters.getMaxYear());
            if (filters.getMinRuntime() != null) filtersPayload.put("minRuntime", filters.getMinRuntime());
            if (filters.getMaxRuntime() != null) filtersPayload.put("maxRuntime", filters.getMaxRuntime());
            if (filters.getCast() != null && !filters.getCast().isEmpty()) {
                filtersPayload.put("cast", filters.getCast());
            }
            if (filters.getDirector() != null && !filters.getDirector().isEmpty()) {
                filtersPayload.put("director", filters.getDirector());
            }
            if (filters.getKeywords() != null && !filters.getKeywords().isEmpty()) {
                filtersPayload.put("keywords", filters.getKeywords());
            }
            if (!filtersPayload.isEmpty()) {
                requestBody.put("filters", filtersPayload);
            }
        }

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

            Movie movie = movieService.findByTmdbId(Long.valueOf(rec.getTmdbId())).orElse(null);
            if (movie == null) {
                try {
                    movie = tmdbService.importMovie(rec.getTmdbId());
                } catch (Exception e) {
                    System.out.println("Skipping recommendation tmdbId=" + rec.getTmdbId() + ": " + e.getMessage());
                    continue;
                }
            }

            MovieResponse response = MovieResponse.fromMovie(movie);
            response.setPredictedRating(rec.getPredictedRating());
            movieResponses.add(response);
        }

        return movieResponses;
    }
}
