package ro.anthony.mediarecommenderapp.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TmdbSearchResponse {
    private Integer page;

    private List<TmdbMovieResponse> results;

    @JsonProperty("total_pages")
    private Integer totalPages;

    @JsonProperty("total_results")
    private Integer totalResults;
}
