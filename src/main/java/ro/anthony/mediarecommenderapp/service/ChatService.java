package ro.anthony.mediarecommenderapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ro.anthony.mediarecommenderapp.dto.MovieResponse;
import ro.anthony.mediarecommenderapp.dto.chat.ChatMessage;
import ro.anthony.mediarecommenderapp.dto.chat.ChatResponse;
import ro.anthony.mediarecommenderapp.dto.chat.LLMRerankReply;
import ro.anthony.mediarecommenderapp.dto.chat.RerankItem;
import ro.anthony.mediarecommenderapp.dto.chat.UserPreferenceProfileDto;
import ro.anthony.mediarecommenderapp.model.Rating;
import ro.anthony.mediarecommenderapp.model.WatchlistItem;
import ro.anthony.mediarecommenderapp.service.llm.LLMService;

import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int CANDIDATE_POOL = 30;
    private static final int RESULT_LIMIT = 10;
    private static final int TOP_RATINGS_LIMIT = 10;
    private static final int WATCHLIST_LIMIT = 20;
    private static final double PYTHON_WEIGHT = 0.5;
    private static final double GEMINI_WEIGHT = 0.5;
    private static final double NEUTRAL_GEMINI_SCORE = 5.0;

    private final RecommendationService recommendationService;
    private final PreferenceProfileService profileService;
    private final RatingService ratingService;
    private final WatchlistService watchlistService;
    private final LLMService llmService;

    public ChatResponse chat(Long userId, List<ChatMessage> history) {
        UserPreferenceProfileDto profile = profileService.getOrGenerate(userId);
        List<MovieResponse> candidates = recommendationService.getCandidates(userId, CANDIDATE_POOL, false);

        String topRatingsText = formatTopRatings(userId);
        String watchlistText = formatWatchlist(userId);

        LLMRerankReply llmReply = llmService.chatAndRerank(
                history, profile, watchlistText, topRatingsText, candidates);

        List<MovieResponse> ranked = mergeAndRank(candidates, llmReply.getRerank());
        return new ChatResponse(llmReply.getReply(), ranked);
    }

    private List<MovieResponse> mergeAndRank(List<MovieResponse> candidates, List<RerankItem> rerank) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<Integer, Double> geminiScores = rerank == null ? Map.of()
                : rerank.stream()
                .filter(r -> r.getTmdbId() != null && r.getScore() != null)
                .collect(Collectors.toMap(RerankItem::getTmdbId, RerankItem::getScore, (a, b) -> a));

        DoubleSummaryStatistics stats = candidates.stream()
                .filter(m -> m.getPredictedRating() != null)
                .mapToDouble(MovieResponse::getPredictedRating)
                .summaryStatistics();
        double min = stats.getMin();
        double range = stats.getMax() - stats.getMin();

        return candidates.stream()
                .peek(m -> {
                    double pyRaw = m.getPredictedRating() == null ? min : m.getPredictedRating();
                    double pyNorm = range > 0 ? (pyRaw - min) / range : 0.5;
                    double gemRaw = geminiScores.getOrDefault(
                            m.getTmdbId() == null ? null : m.getTmdbId().intValue(),
                            NEUTRAL_GEMINI_SCORE);
                    double gemNorm = gemRaw / 10.0;
                    double finalScore = PYTHON_WEIGHT * pyNorm + GEMINI_WEIGHT * gemNorm;
                    m.setPredictedRating((double) Math.round(5 + 5 * finalScore));
                })
                .sorted(Comparator.comparing(MovieResponse::getPredictedRating).reversed())
                .limit(RESULT_LIMIT)
                .toList();
    }

    private String formatTopRatings(Long userId) {
        List<Rating> ratings = ratingService.getRatingsByUser(userId);
        if (ratings.isEmpty()) return "";

        return ratings.stream()
                .sorted(Comparator.comparing(Rating::getScore).reversed())
                .limit(TOP_RATINGS_LIMIT)
                .map(r -> "- \"" + r.getMovie().getTitle() + "\" → " + r.getScore() + "/10")
                .collect(Collectors.joining("\n")) + "\n";
    }

    private String formatWatchlist(Long userId) {
        List<WatchlistItem> items = watchlistService.getUserWatchlist(userId);
        if (items.isEmpty()) return "";

        return items.stream()
                .limit(WATCHLIST_LIMIT)
                .map(w -> "- \"" + w.getMovie().getTitle() + "\" [" + w.getStatus().name() + "]")
                .collect(Collectors.joining("\n")) + "\n";
    }
}
