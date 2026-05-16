package ro.anthony.mediarecommenderapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ro.anthony.mediarecommenderapp.dto.tmdb.TmdbCastMember;
import ro.anthony.mediarecommenderapp.dto.tmdb.TmdbCrewMember;
import ro.anthony.mediarecommenderapp.dto.tmdb.TmdbGenre;
import ro.anthony.mediarecommenderapp.dto.tmdb.TmdbGenreListResponse;
import ro.anthony.mediarecommenderapp.dto.tmdb.TmdbMovieResponse;
import ro.anthony.mediarecommenderapp.dto.tmdb.TmdbSearchResponse;
import ro.anthony.mediarecommenderapp.model.Genre;
import ro.anthony.mediarecommenderapp.model.Movie;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TmdbService {
    private static final int TOP_CAST = 5;

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
                .uri("/movie/{id}?append_to_response=credits", tmdbId)
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

        Movie movie = movieService.findByTmdbId((long) tmdbId).orElseGet(Movie::new);

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

        if (response.getGenres() != null) {
            movie.getGenres().clear();
            for (TmdbGenre tmdbGenre : response.getGenres()) {
                Genre genre = genreService.getGenreByTmdbId(tmdbGenre.getId());
                movie.getGenres().add(genre);
            }
        }

        if (response.getCredits() != null) {
            if (response.getCredits().getCrew() != null) {
                String director = response.getCredits().getCrew().stream()
                        .filter(c -> "Director".equals(c.getJob()))
                        .map(TmdbCrewMember::getName)
                        .findFirst()
                        .orElse(null);
                movie.setDirector(director);
            }
            if (response.getCredits().getCast() != null) {
                String cast = response.getCredits().getCast().stream()
                        .sorted(Comparator.comparing(c -> c.getOrder() == null ? Integer.MAX_VALUE : c.getOrder()))
                        .limit(TOP_CAST)
                        .map(TmdbCastMember::getName)
                        .collect(Collectors.joining(", "));
                movie.setCastNames(cast.isEmpty() ? null : cast);
            }
        }

        return movieService.saveOrUpdate(movie);
    }

    public int importTopRated(int pages) {
        int imported = 0;
        for (int page = 1; page <= pages; page++) {
            TmdbSearchResponse response = tmdbRestClient.get()
                    .uri("/movie/top_rated?page={page}", page)
                    .retrieve()
                    .body(TmdbSearchResponse.class);

            if (response == null || response.getResults() == null) continue;

            for (var tmdbMovie : response.getResults()) {
                try {
                    importMovie(tmdbMovie.getId());
                    imported++;
                } catch (Exception e) {
                    System.out.println("Skipping tmdbId " + tmdbMovie.getId() + ": " + e.getMessage());
                }
            }
        }
        return imported;
    }

    public int refreshAllMovies() {
        List<Movie> all = movieService.getAllMovies();
        int refreshed = 0;
        for (Movie m : all) {
            try {
                importMovie(m.getTmdbId().intValue());
                refreshed++;
            } catch (Exception e) {
                System.out.println("Skipping refresh for tmdbId " + m.getTmdbId() + ": " + e.getMessage());
            }
        }
        return refreshed;
    }

}
