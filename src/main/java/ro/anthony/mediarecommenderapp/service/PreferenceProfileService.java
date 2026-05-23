package ro.anthony.mediarecommenderapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ro.anthony.mediarecommenderapp.dto.chat.UserPreferenceProfileDto;
import ro.anthony.mediarecommenderapp.model.Rating;
import ro.anthony.mediarecommenderapp.model.User;
import ro.anthony.mediarecommenderapp.model.UserPreferenceProfile;
import ro.anthony.mediarecommenderapp.repository.UserPreferenceProfileRepository;
import ro.anthony.mediarecommenderapp.service.llm.LLMService;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PreferenceProfileService {
    private static final int REGENERATION_THRESHOLD = 2;
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private final UserPreferenceProfileRepository repo;
    private final RatingService ratingService;
    private final LLMService llmService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public UserPreferenceProfileDto getOrGenerate(Long userId) {
        List<Rating> reviewedRatings = ratingService.getRatingsByUser(userId).stream()
                .filter(r -> r.getReview() != null && !r.getReview().isBlank())
                .toList();
        int currentReviewCount = reviewedRatings.size();

        Optional<UserPreferenceProfile> cached = repo.findById(userId);
        if (cached.isPresent()
                && Math.abs(currentReviewCount - cached.get().getReviewCountAtGeneration()) < REGENERATION_THRESHOLD) {
            return deserialize(cached.get());
        }

        if (reviewedRatings.isEmpty()) {
            return UserPreferenceProfileDto.empty();
        }

        String reviewsText = formatReviewsForLLM(reviewedRatings);
        UserPreferenceProfileDto generated = llmService.generateProfile(reviewsText);
        save(userId, generated, currentReviewCount);
        return generated;
    }

    private String formatReviewsForLLM(List<Rating> reviewedRatings) {
        StringBuilder sb = new StringBuilder();
        for (Rating r : reviewedRatings) {
            sb.append("- \"").append(r.getMovie().getTitle()).append("\"")
                    .append(" (score: ").append(r.getScore()).append("/10): ")
                    .append(r.getReview().trim()).append("\n");
        }
        return sb.toString();
    }

    private void save(Long userId, UserPreferenceProfileDto dto, int reviewCount) {
        UserPreferenceProfile entity = repo.findById(userId).orElseGet(() -> {
            UserPreferenceProfile newEntity = new UserPreferenceProfile();
            User user = userService.getUserById(userId);
            newEntity.setUser(user);
            return newEntity;
        });

        try {
            entity.setLikesJson(objectMapper.writeValueAsString(safe(dto.getLikes())));
            entity.setDislikesJson(objectMapper.writeValueAsString(safe(dto.getDislikes())));
            entity.setPreferredThemesJson(objectMapper.writeValueAsString(safe(dto.getPreferredThemes())));
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to serialize preference profile", e);
        }
        entity.setReviewCountAtGeneration(reviewCount);
        repo.save(entity);
    }

    private UserPreferenceProfileDto deserialize(UserPreferenceProfile entity) {
        try {
            List<String> likes = objectMapper.readValue(entity.getLikesJson(), STRING_LIST_TYPE);
            List<String> dislikes = objectMapper.readValue(entity.getDislikesJson(), STRING_LIST_TYPE);
            List<String> themes = objectMapper.readValue(entity.getPreferredThemesJson(), STRING_LIST_TYPE);
            return new UserPreferenceProfileDto(likes, dislikes, themes);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to deserialize cached preference profile for user " + entity.getUserId(), e);
        }
    }

    private List<String> safe(List<String> list) {
        return list == null ? List.of() : list;
    }
}
