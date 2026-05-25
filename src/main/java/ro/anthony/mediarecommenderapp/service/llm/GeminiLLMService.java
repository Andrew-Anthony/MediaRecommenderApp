package ro.anthony.mediarecommenderapp.service.llm;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ro.anthony.mediarecommenderapp.dto.MovieResponse;
import ro.anthony.mediarecommenderapp.dto.chat.ChatMessage;
import ro.anthony.mediarecommenderapp.dto.chat.LLMIntentFilters;
import ro.anthony.mediarecommenderapp.dto.chat.LLMRerankReply;
import ro.anthony.mediarecommenderapp.dto.chat.UserPreferenceProfileDto;
import ro.anthony.mediarecommenderapp.dto.chat.gemini.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "gemini", matchIfMissing = true)
@RequiredArgsConstructor
public class GeminiLLMService implements LLMService {
    private final RestClient geminiRestClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model}")
    private String model;

    private static final Map<String, Object> PROFILE_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "likes", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                    "dislikes", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                    "preferredThemes", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))
            ),
            "required", List.of("likes", "dislikes", "preferredThemes")
    );

    private static final Map<String, Object> INTENT_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.ofEntries(
                    Map.entry("wantsRecommendations", Map.of("type", "BOOLEAN")),
                    Map.entry("genres", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))),
                    Map.entry("minYear", Map.of("type", "INTEGER")),
                    Map.entry("maxYear", Map.of("type", "INTEGER")),
                    Map.entry("minRuntime", Map.of("type", "INTEGER")),
                    Map.entry("maxRuntime", Map.of("type", "INTEGER")),
                    Map.entry("cast", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))),
                    Map.entry("director", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))),
                    Map.entry("mood", Map.of("type", "STRING")),
                    Map.entry("similarTo", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")))
            ),
            "required", List.of("wantsRecommendations")
    );

    private static final Map<String, Object> RERANK_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "reply", Map.of("type", "STRING"),
                    "rerank", Map.of(
                            "type", "ARRAY",
                            "items", Map.of(
                                    "type", "OBJECT",
                                    "properties", Map.of(
                                            "tmdbId", Map.of("type", "INTEGER"),
                                            "score", Map.of("type", "NUMBER"),
                                            "reason", Map.of("type", "STRING")
                                    ),
                                    "required", List.of("tmdbId", "score")
                            )
                    )
            ),
            "required", List.of("reply", "rerank")
    );

    @Override
    public UserPreferenceProfileDto generateProfile(String reviewsText) {
        String systemInstruction = """
                You analyze a user's movie reviews and extract a concise preference profile.
                Read all reviews and identify recurring patterns about what the user values or dislikes.
                Output 3-7 short tag-like entries per category (e.g. "psychological complexity", "slow pacing", "strong female leads").
                Be specific to the user's actual reviews, not generic.
                """;

        String userPrompt = "Here are the user's movie reviews:\n\n" + reviewsText
                + "\n\nExtract the preference profile as JSON.";

        String jsonText = callGemini(
                List.of(new ChatMessage("user", userPrompt)),
                systemInstruction,
                PROFILE_SCHEMA
        );

        try {
            return objectMapper.readValue(jsonText, UserPreferenceProfileDto.class);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to parse Gemini profile response: " + jsonText, e);
        }
    }

    @Override
    public LLMIntentFilters extractIntent(List<ChatMessage> history, UserPreferenceProfileDto profile) {
        String systemInstruction = buildIntentSystemInstruction(profile);
        String jsonText = callGemini(history, systemInstruction, INTENT_SCHEMA, 0.1);

        try {
            return objectMapper.readValue(jsonText, LLMIntentFilters.class);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to parse Gemini intent response: " + jsonText, e);
        }
    }

    private String buildIntentSystemInstruction(UserPreferenceProfileDto profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("You extract movie recommendation filters from the user's message.\n\n");

        sb.append("ALWAYS include \"wantsRecommendations\":\n");
        sb.append("- true: user wants movies suggested\n");
        sb.append("- false: greetings, chitchat, questions about how you work (omit ALL other fields)\n\n");

        sb.append("Fields to extract when wantsRecommendations=true (omit any field you don't extract):\n");
        sb.append("- genres: from the exact list below\n");
        sb.append("- minYear, maxYear: release year bounds\n");
        sb.append("- minRuntime, maxRuntime: minutes\n");
        sb.append("- cast: actor full names mentioned\n");
        sb.append("- director: director full names mentioned (distinct from cast!)\n");
        sb.append("- mood: free-form descriptor like \"fun\", \"dark\", \"relaxant\"\n");
        sb.append("- similarTo: movie titles the user references\n\n");

        sb.append("GENRES (exact spelling, MovieLens taxonomy — use \"Sci-Fi\" not \"Science Fiction\", \"Children\" not \"Family\", \"Musical\" not \"Music\"):\n");
        sb.append("Action, Adventure, Animation, Children, Comedy, Crime, Documentary, Drama, Fantasy, ");
        sb.append("Film-Noir, Horror, Musical, Mystery, Romance, Sci-Fi, Thriller, War, Western\n\n");

        sb.append("EXAMPLES — study these patterns:\n\n");

        sb.append("User: \"salut\"\n");
        sb.append("{\"wantsRecommendations\": false}\n\n");

        sb.append("User: \"vreau ceva fun din anii 80\"\n");
        sb.append("{\"wantsRecommendations\": true, \"minYear\": 1980, \"maxYear\": 1989, \"mood\": \"fun\"}\n\n");

        sb.append("User: \"recomandă-mi un thriller\"\n");
        sb.append("{\"wantsRecommendations\": true, \"genres\": [\"Thriller\"]}\n\n");

        sb.append("User: \"film de Tarantino cu DiCaprio\"\n");
        sb.append("{\"wantsRecommendations\": true, \"director\": [\"Quentin Tarantino\"], \"cast\": [\"Leonardo DiCaprio\"]}\n\n");

        sb.append("User: \"poti sa imi dai filme regizate de Tarantino in care joaca Uma Thurman\"\n");
        sb.append("{\"wantsRecommendations\": true, \"director\": [\"Quentin Tarantino\"], \"cast\": [\"Uma Thurman\"]}\n\n");

        sb.append("User: \"un anime relaxant\"\n");
        sb.append("{\"wantsRecommendations\": true, \"genres\": [\"Animation\"], \"mood\": \"relaxant\"}\n\n");

        sb.append("User: \"ceva ca Inception dar mai recent\"\n");
        sb.append("{\"wantsRecommendations\": true, \"minYear\": 2018, \"similarTo\": [\"Inception\"]}\n\n");

        sb.append("User: \"film de Miyazaki\"\n");
        sb.append("{\"wantsRecommendations\": true, \"director\": [\"Hayao Miyazaki\"]}\n\n");

        sb.append("User: \"un Western din anii 90\"\n");
        sb.append("{\"wantsRecommendations\": true, \"genres\": [\"Western\"], \"minYear\": 1990, \"maxYear\": 1999}\n\n");

        if (profile != null && !profile.isEmpty()) {
            sb.append("USER PROFILE (context only, do NOT auto-add as filters):\n");
            sb.append("- Likes: ").append(String.join(", ", profile.getLikes())).append("\n");
            sb.append("- Themes: ").append(String.join(", ", profile.getPreferredThemes())).append("\n\n");
        }

        sb.append("CRITICAL — read carefully before responding:\n");
        sb.append("1. OMIT fields you don't extract. NEVER output null, 0, [], or \"\".\n");
        sb.append("2. Extract ALL applicable filters. If user mentions BOTH an actor and a director, extract BOTH — they coexist.\n");
        sb.append("3. NEVER invent year ranges. Only set year fields when user mentions a time period.\n");
        sb.append("4. \"cu X\" / \"with X\" / \"joaca X\" / \"starring X\" → cast. \"regizat de X\" / \"de X\" / \"directed by X\" → director.\n");
        sb.append("5. For decades always set BOTH bounds: \"anii 80\" → minYear=1980 AND maxYear=1989.\n");
        sb.append("6. Use full actor/director names: \"Tarantino\" → \"Quentin Tarantino\"; \"Nolan\" → \"Christopher Nolan\".\n");
        return sb.toString();
    }

    @Override
    public LLMRerankReply chatAndRerank(
            List<ChatMessage> history,
            UserPreferenceProfileDto profile,
            String watchlistText,
            String topRatingsText,
            List<MovieResponse> candidates
    ) {
        String systemInstruction = buildChatSystemInstruction(profile, watchlistText, topRatingsText, candidates);

        String jsonText = callGemini(history, systemInstruction, RERANK_SCHEMA);

        try {
            return objectMapper.readValue(jsonText, LLMRerankReply.class);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to parse Gemini rerank response: " + jsonText, e);
        }
    }

    private String callGemini(List<ChatMessage> history, String systemInstructionText, Map<String, Object> schema) {
        return callGemini(history, systemInstructionText, schema, 0.7);
    }

    private String callGemini(List<ChatMessage> history, String systemInstructionText, Map<String, Object> schema, double temperature) {
        List<GeminiContent> contents = new ArrayList<>();
        for (ChatMessage m : history) {
            String role = "assistant".equals(m.getRole()) ? "model" : "user";
            contents.add(new GeminiContent(role, List.of(new GeminiPart(m.getContent()))));
        }

        GeminiRequest body = new GeminiRequest();
        body.setContents(contents);
        body.setSystemInstruction(new GeminiContent(null, List.of(new GeminiPart(systemInstructionText))));

        GeminiGenerationConfig genCfg = new GeminiGenerationConfig();
        genCfg.setResponseMimeType("application/json");
        genCfg.setResponseSchema(schema);
        genCfg.setTemperature(temperature);
        body.setGenerationConfig(genCfg);

        GeminiResponse resp = geminiRestClient.post()
                .uri("/models/{model}:generateContent?key={key}", model, apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(GeminiResponse.class);

        if (resp == null || resp.getCandidates() == null || resp.getCandidates().isEmpty()) {
            throw new RuntimeException("Empty response from Gemini");
        }
        GeminiCandidate candidate = resp.getCandidates().getFirst();
        if (candidate.getContent() == null || candidate.getContent().getParts() == null
                || candidate.getContent().getParts().isEmpty()) {
            throw new RuntimeException("Gemini returned no content (finishReason=" + candidate.getFinishReason() + ")");
        }
        return candidate.getContent().getParts().getFirst().getText();
    }

    private String buildChatSystemInstruction(
            UserPreferenceProfileDto profile,
            String watchlistText,
            String topRatingsText,
            List<MovieResponse> candidates
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a personalized movie recommendation assistant.\n");
        sb.append("Always reply in the same language the user wrote in (Romanian if they write Romanian, English if English, etc.).\n\n");

        sb.append("USER PREFERENCE PROFILE:\n");
        if (profile == null || profile.isEmpty()) {
            sb.append("(no written reviews yet — rely on top ratings below)\n");
        } else {
            sb.append("- Likes: ").append(String.join(", ", profile.getLikes())).append("\n");
            sb.append("- Dislikes: ").append(String.join(", ", profile.getDislikes())).append("\n");
            sb.append("- Preferred themes: ").append(String.join(", ", profile.getPreferredThemes())).append("\n");
        }
        sb.append("\n");

        sb.append("USER'S TOP RATINGS:\n");
        sb.append(topRatingsText.isBlank() ? "(none)\n" : topRatingsText).append("\n");

        sb.append("USER'S WATCHLIST:\n");
        sb.append(watchlistText.isBlank() ? "(empty)\n" : watchlistText).append("\n");

        sb.append("CANDIDATE MOVIES (already filtered by the recommender engine, excluding watchlist):\n");
        if (candidates.isEmpty()) {
            sb.append("(no candidates — user has no ratings yet)\n");
        } else {
            for (MovieResponse m : candidates) {
                sb.append("- tmdbId=").append(m.getTmdbId())
                        .append(" | ").append(m.getTitle())
                        .append(" | genres: ").append(m.getGenres() == null ? "" : String.join(", ", m.getGenres()))
                        .append(" | tmdb avg: ").append(m.getVoteAverage());
                if (m.getOverview() != null && !m.getOverview().isBlank()) {
                    String overview = m.getOverview();
                    if (overview.length() > 200) overview = overview.substring(0, 200) + "...";
                    sb.append(" | overview: ").append(overview);
                }
                sb.append("\n");
            }
        }
        sb.append("\n");

        sb.append("YOUR TASK:\n");
        sb.append("1. Write a short conversational reply addressing the user's last message (1-3 sentences).\n");
        sb.append("2. For EACH candidate above, output a score 0-10 reflecting how well it fits the user's profile AND their current request. ");
        sb.append("Use the full range: 0-3 = poor fit, 4-6 = neutral, 7-8 = good fit, 9-10 = excellent fit. ");
        sb.append("Include a short 'reason' (max 1 sentence) for each.\n");
        sb.append("3. If there are no candidates, return an empty rerank list and just reply conversationally.\n");
        return sb.toString();
    }
}
