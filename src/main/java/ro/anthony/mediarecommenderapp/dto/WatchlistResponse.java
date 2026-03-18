package ro.anthony.mediarecommenderapp.dto;

import lombok.Getter;
import lombok.Setter;
import ro.anthony.mediarecommenderapp.model.WatchlistStatus;

import java.time.LocalDateTime;

@Getter
@Setter
public class WatchlistResponse {
    private Long id;
    private Long userId;
    private String username;
    private Long movieId;
    private String movieTitle;
    private WatchlistStatus status;
    private LocalDateTime addedAt;
}
