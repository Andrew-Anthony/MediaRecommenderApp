package ro.anthony.mediarecommenderapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.anthony.mediarecommenderapp.dto.MovieResponse;
import ro.anthony.mediarecommenderapp.service.RecommendationService;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<List<MovieResponse>> getRecommendations(@RequestParam("user_id") Long userId, @RequestParam(defaultValue = "10") int n) {
        List<MovieResponse> recommendations = recommendationService.getRecommendations(userId, n);
        return ResponseEntity.ok(recommendations);
    }
}
