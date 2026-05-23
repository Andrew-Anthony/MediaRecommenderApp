package ro.anthony.mediarecommenderapp.dto.chat.gemini;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeminiRequest {
    private List<GeminiContent> contents;
    private GeminiContent systemInstruction;
    private GeminiGenerationConfig generationConfig;
}
