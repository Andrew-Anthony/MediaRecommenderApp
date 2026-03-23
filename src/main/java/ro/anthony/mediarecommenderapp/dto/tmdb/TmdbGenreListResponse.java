package ro.anthony.mediarecommenderapp.dto.tmdb;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TmdbGenreListResponse {
    private List<TmdbGenre> genres;
}
