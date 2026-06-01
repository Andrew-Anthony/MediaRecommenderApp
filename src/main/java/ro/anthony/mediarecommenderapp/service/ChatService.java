package ro.anthony.mediarecommenderapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ro.anthony.mediarecommenderapp.dto.MovieResponse;
import ro.anthony.mediarecommenderapp.dto.chat.ChatMessage;
import ro.anthony.mediarecommenderapp.dto.chat.ChatResponse;
import ro.anthony.mediarecommenderapp.dto.chat.LLMIntentFilters;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int CANDIDATE_POOL = 50;
    private static final int RESULT_LIMIT_MAX = 8;
    private static final int RESULT_LIMIT_MIN = 1;
    private static final double QUALITY_THRESHOLD = 7.0;
    private static final double GEMINI_VETO_THRESHOLD = 4.0;
    private static final int TOP_RATINGS_LIMIT = 10;
    private static final int WATCHLIST_LIMIT = 20;
    private static final double PYTHON_WEIGHT = 0.5;
    private static final double GEMINI_WEIGHT = 0.5;
    private static final double NEUTRAL_GEMINI_SCORE = 5.0;
    private static final double MIN_TMDB_RATING = 6.5;

    private static final Pattern DECADE_RO = Pattern.compile("anii\\s+(\\d{2,4})", Pattern.CASE_INSENSITIVE);
    private static final Pattern DECADE_EN = Pattern.compile("\\b(?:the\\s+)?(\\d{2,4})s\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern BEFORE_YEAR = Pattern.compile(
            "(?:înainte de|inainte de|before|older than|prior to|până în|pana in|pre-?)\\s*(\\d{4})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern AFTER_YEAR = Pattern.compile(
            "(?:după|dupa|after|since|newer than|later than|de la|peste|începând cu|incepand cu|post-?)\\s*(\\d{4})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern EXACT_YEAR = Pattern.compile(
            "(?:din anul|din|în|in|from|of|anul|prin|during|released in)\\s+(\\d{4})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ACTOR_REFERENCE = Pattern.compile(
            "\\b(?:cu|joacă|joaca|în care joacă|în care joaca|with|starring|featuring|stars)\\s+[A-ZȘȚĂÂÎ]",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final String SEARCH_HINT =
            "\n\n💡 Pentru combinații atât de specifice, pagina Search poate fi mai precisă.";

    private static final int RECENT_FEEDBACK_LIMIT = 10;

    private final RecommendationService recommendationService;
    private final PreferenceProfileService profileService;
    private final RatingService ratingService;
    private final WatchlistService watchlistService;
    private final ChatFeedbackService chatFeedbackService;
    private final LLMService llmService;

    public ChatResponse chat(Long userId, List<ChatMessage> history) {
        UserPreferenceProfileDto profile = profileService.getOrGenerate(userId);

        LLMIntentFilters filters = llmService.extractIntent(history, profile);
        boolean wantsRecommendations = Boolean.TRUE.equals(filters.getWantsRecommendations());

        if (wantsRecommendations) {
            sanitizeFilters(filters);
            applyYearOverrides(filters, lastUserMessage(history));
        }

        System.out.println("[Chat] Extracted intent: wantsRecs=" + wantsRecommendations
                + ", genres=" + filters.getGenres()
                + ", year=[" + filters.getMinYear() + "," + filters.getMaxYear() + "]"
                + ", runtime=[" + filters.getMinRuntime() + "," + filters.getMaxRuntime() + "]"
                + ", cast=" + filters.getCast()
                + ", director=" + filters.getDirector()
                + ", keywords=" + filters.getKeywords()
                + ", mood=" + filters.getMood()
                + ", similarTo=" + filters.getSimilarTo());

        List<MovieResponse> candidates = wantsRecommendations
                ? recommendationService.getCandidates(userId, CANDIDATE_POOL, true, filters)
                : List.of();

        String topRatingsText = formatTopRatings(userId);
        String watchlistText = formatWatchlist(userId);
        String feedbackText = chatFeedbackService.formatForPrompt(userId, RECENT_FEEDBACK_LIMIT);
        System.out.println("[Chat] Recent feedback context:\n" + (feedbackText.isBlank() ? "(none)" : feedbackText));

        LLMRerankReply llmReply = llmService.chatAndRerank(
                history, profile, watchlistText, topRatingsText, feedbackText, candidates);

        List<MovieResponse> ranked = wantsRecommendations
                ? mergeAndRank(candidates, llmReply.getRerank())
                : List.of();

        String reply = llmReply.getReply();
        if (wantsRecommendations && isNarrowRequest(filters, lastUserMessage(history))) {
            reply = reply + SEARCH_HINT;
        }
        return new ChatResponse(reply, ranked);
    }

    private void sanitizeFilters(LLMIntentFilters filters) {
        if (filters.getMinYear() != null && filters.getMinYear() <= 0) filters.setMinYear(null);
        if (filters.getMaxYear() != null && filters.getMaxYear() <= 0) filters.setMaxYear(null);
        if (filters.getMinRuntime() != null && filters.getMinRuntime() <= 0) filters.setMinRuntime(null);
        if (filters.getMaxRuntime() != null && filters.getMaxRuntime() <= 0) filters.setMaxRuntime(null);
        if (filters.getMood() != null && filters.getMood().isBlank()) filters.setMood(null);
        if (filters.getGenres() != null && filters.getGenres().isEmpty()) filters.setGenres(null);
        if (filters.getCast() != null && filters.getCast().isEmpty()) filters.setCast(null);
        if (filters.getDirector() != null && filters.getDirector().isEmpty()) filters.setDirector(null);
        if (filters.getKeywords() != null && filters.getKeywords().isEmpty()) filters.setKeywords(null);
        if (filters.getKeywords() != null && filters.getKeywords().size() > 4) filters.setKeywords(null);
        if (filters.getSimilarTo() != null && filters.getSimilarTo().isEmpty()) filters.setSimilarTo(null);
    }

    private boolean isNarrowRequest(LLMIntentFilters filters, String lastMessage) {
        boolean castSet = filters.getCast() != null && !filters.getCast().isEmpty();
        boolean directorSet = filters.getDirector() != null && !filters.getDirector().isEmpty();

        if (castSet && directorSet) return true;

        return directorSet && lastMessage != null && ACTOR_REFERENCE.matcher(lastMessage).find();
    }

    private List<MovieResponse> mergeAndRank(List<MovieResponse> candidates, List<RerankItem> rerank) {
        candidates = candidates.stream()
                .filter(m -> m.getVoteAverage() != null && m.getVoteAverage() >= MIN_TMDB_RATING)
                .toList();

        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<Integer, Double> geminiScores = rerank == null ? Map.of()
                : rerank.stream()
                .filter(r -> r.getTmdbId() != null && r.getScore() != null)
                .collect(Collectors.toMap(RerankItem::getTmdbId, RerankItem::getScore, (a, b) -> a));

        List<MovieResponse> notRejected = candidates.stream()
                .filter(m -> {
                    Double g = m.getTmdbId() == null ? null : geminiScores.get(m.getTmdbId().intValue());
                    return g == null || g >= GEMINI_VETO_THRESHOLD;
                })
                .toList();
        if (!notRejected.isEmpty()) {
            candidates = notRejected;
        }

        DoubleSummaryStatistics stats = candidates.stream()
                .filter(m -> m.getPredictedRating() != null)
                .mapToDouble(MovieResponse::getPredictedRating)
                .summaryStatistics();
        double min = stats.getMin();
        double range = stats.getMax() - stats.getMin();

        List<MovieResponse> sorted = candidates.stream()
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
                .toList();

        List<MovieResponse> highQuality = sorted.stream()
                .filter(m -> m.getPredictedRating() >= QUALITY_THRESHOLD)
                .limit(RESULT_LIMIT_MAX)
                .toList();

        if (!highQuality.isEmpty()) {
            return highQuality;
        }
        return sorted.stream().limit(RESULT_LIMIT_MIN).toList();
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

    private String lastUserMessage(List<ChatMessage> history) {
        for (int i = history.size() - 1; i >= 0; i--) {
            if ("user".equals(history.get(i).getRole())) {
                return history.get(i).getContent();
            }
        }
        return null;
    }

    private void applyYearOverrides(LLMIntentFilters filters, String message) {
        if (message == null) return;

        Integer decadeStart = detectDecadeStart(message);
        if (decadeStart != null) {
            filters.setMinYear(decadeStart);
            filters.setMaxYear(decadeStart + 9);
            return;
        }

        Matcher before = BEFORE_YEAR.matcher(message);
        if (before.find()) {
            int y = Integer.parseInt(before.group(1));
            if (isValidYear(y)) {
                filters.setMinYear(null);
                filters.setMaxYear(y);
            }
            return;
        }

        Matcher after = AFTER_YEAR.matcher(message);
        if (after.find()) {
            int y = Integer.parseInt(after.group(1));
            if (isValidYear(y)) {
                filters.setMinYear(y);
                filters.setMaxYear(null);
            }
            return;
        }

        Matcher exact = EXACT_YEAR.matcher(message);
        if (exact.find()) {
            int y = Integer.parseInt(exact.group(1));
            if (isValidYear(y)) {
                filters.setMinYear(y);
                filters.setMaxYear(y);
            }
        }
    }

    private boolean isValidYear(int y) {
        return y >= 1900 && y <= 2029;
    }

    private Integer detectDecadeStart(String text) {
        Matcher m = DECADE_RO.matcher(text);
        if (m.find()) return normalizeDecade(Integer.parseInt(m.group(1)));

        m = DECADE_EN.matcher(text);
        if (m.find()) return normalizeDecade(Integer.parseInt(m.group(1)));

        return null;
    }

    private Integer normalizeDecade(int n) {
        if (n >= 1900 && n <= 2099) return (n / 10) * 10;
        if (n >= 10 && n <= 99) {
            return n < 30 ? 2000 + n : 1900 + n;
        }
        return null;
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
