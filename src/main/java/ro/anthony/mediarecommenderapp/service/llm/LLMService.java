package ro.anthony.mediarecommenderapp.service.llm;

import ro.anthony.mediarecommenderapp.dto.MovieResponse;
import ro.anthony.mediarecommenderapp.dto.chat.ChatMessage;
import ro.anthony.mediarecommenderapp.dto.chat.LLMIntentFilters;
import ro.anthony.mediarecommenderapp.dto.chat.LLMRerankReply;
import ro.anthony.mediarecommenderapp.dto.chat.UserPreferenceProfileDto;

import java.util.List;

public interface LLMService {
    UserPreferenceProfileDto generateProfile(String reviewsText, String feedbackText);

    LLMIntentFilters extractIntent(List<ChatMessage> history, UserPreferenceProfileDto profile);

    LLMRerankReply chatAndRerank(
            List<ChatMessage> history,
            UserPreferenceProfileDto profile,
            String watchlistText,
            String topRatingsText,
            String feedbackText,
            List<MovieResponse> candidates
    );
}
