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
        sb.append("You analyze the user's latest message (in conversation context) and extract HARD FILTERS ");
        sb.append("that will be applied to narrow down the recommendation pool.\n\n");

        sb.append("CRITICAL: ALWAYS set `wantsRecommendations`:\n");
        sb.append("- true: user is asking for movie suggestions (\"recomandă\", \"vreau ceva\", \"caut un film\", \"give me a movie\", etc.)\n");
        sb.append("- false: user is just chatting (\"salut\", \"mulțumesc\", \"cum funcționezi\", \"hello\", asking about the app, etc.)\n");
        sb.append("If false, OMIT all other fields (no filtering needed for a chat reply).\n\n");

        sb.append("FILTER EXTRACTION RULES (only when wantsRecommendations=true):\n");
        sb.append("- For fields NOT implied by the user, omit them.\n");
        sb.append("- For fields the user DID imply, you MUST extract them — do not be overly cautious.\n\n");

        sb.append("DECADE RULE (CRITICAL — always set BOTH bounds for a decade):\n");
        sb.append("- \"anii 80\" / \"80s\" / \"the 80s\" / \"eighties\" → {\"minYear\":1980,\"maxYear\":1989}\n");
        sb.append("- \"anii 90\" / \"90s\" / \"nineties\" → {\"minYear\":1990,\"maxYear\":1999}\n");
        sb.append("- \"anii 2000\" / \"2000s\" → {\"minYear\":2000,\"maxYear\":2009}\n");
        sb.append("- \"anii 70\" / \"70s\" → {\"minYear\":1970,\"maxYear\":1979}\n");
        sb.append("- A decade reference is a STRICT bound. Setting only minYear is WRONG.\n\n");

        sb.append("OTHER MAPPINGS:\n");
        sb.append("- \"recent\" / \"new\" / \"din ultimii ani\" → minYear=2018 (no maxYear).\n");
        sb.append("- \"old\" / \"clasice\" / \"vechi\" → maxYear=1990 (no minYear).\n");
        sb.append("- \"short\" / \"scurt\" → maxRuntime=120; \"long\" / \"lung\" → minRuntime=150.\n");
        sb.append("- \"animated\" / \"desene\" / \"animat\" → genres=[Animation].\n");
        sb.append("- ACTORS: any person referenced by name as an actor → cast=[\"Full Name\"]. ");
        sb.append("Examples: \"film cu Jackie Chan\" → cast=[\"Jackie Chan\"]; \"with Tom Hanks\" → cast=[\"Tom Hanks\"]; ");
        sb.append("\"cu De Niro și Pacino\" → cast=[\"Robert De Niro\", \"Al Pacino\"]. ");
        sb.append("Always use the FULL name as commonly known (\"Robert De Niro\", not \"De Niro\").\n\n");

        sb.append("EXAMPLE — user says \"vreau ceva fun din anii 80\":\n");
        sb.append("{\"wantsRecommendations\":true,\"minYear\":1980,\"maxYear\":1989,\"mood\":\"fun\"}\n\n");

        sb.append("AVAILABLE GENRES (use EXACT spelling, case-sensitive — MovieLens taxonomy):\n");
        sb.append("Action, Adventure, Animation, Children, Comedy, Crime, Documentary, Drama, Fantasy, ");
        sb.append("Film-Noir, Horror, Musical, Mystery, Romance, Sci-Fi, Thriller, War, Western\n");
        sb.append("(Note: use \"Sci-Fi\" not \"Science Fiction\"; \"Children\" not \"Family\"; \"Musical\" not \"Music\".)\n\n");

        sb.append("FIELD MEANINGS:\n");
        sb.append("- genres: movie must have AT LEAST ONE of these (OR logic)\n");
        sb.append("- minYear / maxYear: release year bounds\n");
        sb.append("- minRuntime / maxRuntime: in minutes\n");
        sb.append("- mood: free-form descriptor like \"relaxant\", \"intens\", \"feel-good\" (used as context, not a hard filter)\n");
        sb.append("- similarTo: list of movie titles the user referenced (e.g. \"like Inception\")\n\n");

        if (profile != null && !profile.isEmpty()) {
            sb.append("USER'S GENERAL PREFERENCES (context only — DO NOT add these as filters unless the user asks):\n");
            sb.append("- Likes: ").append(String.join(", ", profile.getLikes())).append("\n");
            sb.append("- Preferred themes: ").append(String.join(", ", profile.getPreferredThemes())).append("\n\n");
        }

        sb.append("Return ONLY the relevant fields as JSON. If the user is just chatting or hasn't asked for anything specific, return {}.");
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
