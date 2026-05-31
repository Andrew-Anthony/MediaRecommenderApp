package ro.anthony.mediarecommenderapp.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ro.anthony.mediarecommenderapp.model.FeedbackSignal;

@Getter
@AllArgsConstructor
public class ChatFeedbackView {
    private Integer tmdbId;
    private FeedbackSignal signal;
}
