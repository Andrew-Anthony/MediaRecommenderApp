package ro.anthony.mediarecommenderapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.anthony.mediarecommenderapp.dto.GenreResponse;
import ro.anthony.mediarecommenderapp.model.Genre;
import ro.anthony.mediarecommenderapp.service.GenreService;

import java.util.List;

@RestController
@RequestMapping("/api/genres")
@RequiredArgsConstructor
public class GenreController {
    private final GenreService genreService;

    private GenreResponse toGenreResponse(Genre genre) {
        GenreResponse genreResponse = new GenreResponse();
        genreResponse.setId(genre.getId());
        genreResponse.setName(genre.getName());
        return genreResponse;
    }

    @GetMapping
    public ResponseEntity<List<GenreResponse>> getAllGenres() {
        List<Genre> genreList = genreService.getAllGenres();
        List<GenreResponse> genreResponseList = genreList.stream().map(this::toGenreResponse).toList();
        return ResponseEntity.ok(genreResponseList);
    }
}
