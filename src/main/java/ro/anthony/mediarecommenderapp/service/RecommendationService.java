package ro.anthony.mediarecommenderapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ro.anthony.mediarecommenderapp.dto.MovieResponse;
import ro.anthony.mediarecommenderapp.dto.RecommendationResponse;
import ro.anthony.mediarecommenderapp.model.Movie;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final RestClient recommendationRestClient;
    private final MovieService movieService;
    private final TmdbService tmdbService;

    public List<MovieResponse> getRecommendations(int userId, int n) {
        List<RecommendationResponse> recommendations = recommendationRestClient.get()
                .uri("/recommend?user_id={userId}&n={n}", userId, n)
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
