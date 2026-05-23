package ro.anthony.mediarecommenderapp.dto.chat.gemini;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeminiCandidate {
    private GeminiContent content;
    private String finishReason;
}
