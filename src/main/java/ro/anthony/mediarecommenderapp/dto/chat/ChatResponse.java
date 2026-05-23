package ro.anthony.mediarecommenderapp.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ro.anthony.mediarecommenderapp.dto.MovieResponse;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String reply;
    private List<MovieResponse> movies;
}
