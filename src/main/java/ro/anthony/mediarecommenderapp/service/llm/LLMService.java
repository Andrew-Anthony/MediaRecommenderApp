package ro.anthony.mediarecommenderapp.service.llm;

import ro.anthony.mediarecommenderapp.dto.MovieResponse;
import ro.anthony.mediarecommenderapp.dto.chat.ChatMessage;
import ro.anthony.mediarecommenderapp.dto.chat.LLMRerankReply;
import ro.anthony.mediarecommenderapp.dto.chat.UserPreferenceProfileDto;

import java.util.List;

public interface LLMService {
    UserPreferenceProfileDto generateProfile(String reviewsText);

    LLMRerankReply chatAndRerank(
            List<ChatMessage> history,
            UserPreferenceProfileDto profile,
            String watchlistText,
            String topRatingsText,
            List<MovieResponse> candidates
    );
}
