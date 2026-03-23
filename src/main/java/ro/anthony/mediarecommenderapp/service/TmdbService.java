package ro.anthony.mediarecommenderapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ro.anthony.mediarecommenderapp.dto.tmdb.TmdbGenre;
import ro.anthony.mediarecommenderapp.dto.tmdb.TmdbGenreListResponse;
import ro.anthony.mediarecommenderapp.dto.tmdb.TmdbMovieResponse;
import ro.anthony.mediarecommenderapp.dto.tmdb.TmdbSearchResponse;
import ro.anthony.mediarecommenderapp.model.Genre;
import ro.anthony.mediarecommenderapp.model.Movie;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TmdbService {
    private final RestClient tmdbRestClient;
    private final MovieService movieService;
    private final GenreService genreService;

    public TmdbSearchResponse searchMovies(String query) {
        return tmdbRestClient.get()
                .uri("/search/movie?query={query}", query)
                .retrieve()
                .body(TmdbSearchResponse.class);
    }

    public TmdbMovieResponse getMovieDetails(int tmdbId) {
        return tmdbRestClient.get()
                .uri("/movie/{id}", tmdbId)
                .retrieve()
                .body(TmdbMovieResponse.class);
    }

    public TmdbGenreListResponse fetchGenres() {
        return tmdbRestClient.get()
                .uri("/genre/movie/list")
                .retrieve()
                .body(TmdbGenreListResponse.class);
    }

    public void syncGenres() {
        TmdbGenreListResponse response = fetchGenres();

        for(TmdbGenre tmdbGenre : response.getGenres()) {
            Genre genre = new Genre();
            genre.setTmdbId(tmdbGenre.getId());
            genre.setName(tmdbGenre.getName());

            genreService.saveGenre(genre);
        }
    }

    public Movie importMovie(int tmdbId) {
        TmdbMovieResponse response = getMovieDetails(tmdbId);

        Movie movie = new Movie();
        movie.setTmdbId(Long.valueOf(response.getId()));
        movie.setTitle(response.getTitle());
        movie.setOriginalTitle(response.getOriginalTitle());
        movie.setOriginalLanguage(response.getOriginalLanguage());
        movie.setOverview(response.getOverview());
        movie.setPosterPath(response.getPosterPath());
        movie.setBackdropPath(response.getBackdropPath());

        if(response.getReleaseDate() != null && !response.getReleaseDate().isBlank()) {
            movie.setReleaseDate(LocalDate.parse(response.getReleaseDate()));
        }

        movie.setRuntime(response.getRuntime());
        movie.setVoteAverage(response.getVoteAverage());
        movie.setVoteCount(response.getVoteCount());
        movie.setPopularity(response.getPopularity());

        if(response.getGenreIds() != null) {
            for(Integer genreId : response.getGenreIds()) {
                Genre genre = genreService.getGenreByTmdbId(genreId);
                movie.getGenres().add(genre);
            }
        }

        return movieService.saveMovie(movie);
    }

}
