package ro.anthony.mediarecommenderapp.dto.chat;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import ro.anthony.mediarecommenderapp.model.FeedbackSignal;

@Getter
@Setter
public class ChatFeedbackRequest {
    @NotNull
    private Integer tmdbId;

    @NotNull
    private FeedbackSignal signal;
}
