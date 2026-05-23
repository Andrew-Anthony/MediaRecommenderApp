package ro.anthony.mediarecommenderapp.dto.chat.gemini;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeminiGenerationConfig {
    private String responseMimeType;
    private Map<String, Object> responseSchema;
    private Double temperature;
}
