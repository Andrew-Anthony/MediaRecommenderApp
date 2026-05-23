package ro.anthony.mediarecommenderapp.dto.chat.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponse {
    private List<GeminiCandidate> candidates;
}
