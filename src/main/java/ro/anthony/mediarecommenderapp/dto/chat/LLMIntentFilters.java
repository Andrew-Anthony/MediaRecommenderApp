package ro.anthony.mediarecommenderapp.dto.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LLMIntentFilters {

    private Boolean wantsRecommendations;

    private List<String> genres;
    private Integer minYear;
    private Integer maxYear;
    private Integer maxRuntime;
    private Integer minRuntime;
    private List<String> cast;
    private List<String> director;
    private List<String> keywords;

    private String mood;
    private List<String> similarTo;
}
