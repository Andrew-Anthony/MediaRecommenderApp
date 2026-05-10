package ro.anthony.mediarecommenderapp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.anthony.mediarecommenderapp.dto.WatchlistRequest;
import ro.anthony.mediarecommenderapp.dto.WatchlistResponse;
import ro.anthony.mediarecommenderapp.model.WatchlistItem;
import ro.anthony.mediarecommenderapp.model.WatchlistStatus;
import ro.anthony.mediarecommenderapp.service.WatchlistService;
import ro.anthony.mediarecommenderapp.security.AuthorizationService;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/watchlist")
@RequiredArgsConstructor
public class WatchlistController {
    private final WatchlistService watchlistService;
    private final AuthorizationService authorizationService;

    private WatchlistResponse toWatchlistResponse(WatchlistItem watchlistItem) {
        WatchlistResponse watchlistResponse = new WatchlistResponse();
        watchlistResponse.setId(watchlistItem.getId());
        watchlistResponse.setUserId(watchlistItem.getUser().getId());
        watchlistResponse.setUsername(watchlistItem.getUser().getUsername());
        watchlistResponse.setMovieId(watchlistItem.getMovie().getId());
        watchlistResponse.setMovieTitle(watchlistItem.getMovie().getTitle());
        watchlistResponse.setStatus(watchlistItem.getStatus());
        watchlistResponse.setAddedAt(watchlistItem.getAddedAt());
        return watchlistResponse;
    }

    @PostMapping
    public ResponseEntity<WatchlistResponse> addToWatchlist(@PathVariable Long userId,
                                                            @Valid @RequestBody WatchlistRequest request) {
        authorizationService.verifyOwnership(userId);
        WatchlistItem watchlistItem = watchlistService.addToWatchlist(
                userId,
                request.getMovieId(),
                request.getStatus()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toWatchlistResponse(watchlistItem));
    }

    @GetMapping
    public ResponseEntity<List<WatchlistResponse>> getWatchlist(@PathVariable Long userId,
                                                                @RequestParam(required = false) WatchlistStatus status) {
        authorizationService.verifyOwnership(userId);
        List<WatchlistItem> watchlistItemList;

        if(status != null) {
            watchlistItemList = watchlistService.getUserWatchlistByStatus(userId, status);
        } else {
            watchlistItemList = watchlistService.getUserWatchlist(userId);
        }

        List<WatchlistResponse> watchlistResponseList = watchlistItemList.stream().map(this::toWatchlistResponse).toList();
        return ResponseEntity.ok(watchlistResponseList);
    }

    @PutMapping("/{movieId}")
    public ResponseEntity<WatchlistResponse> changeWatchlistItemStatus(@PathVariable Long userId, @PathVariable Long movieId,
                                                                       @Valid @RequestBody WatchlistRequest request) {
        authorizationService.verifyOwnership(userId);
        WatchlistItem watchlistItem = watchlistService.updateWatchlistStatus(
                userId,
                movieId,
                request.getStatus()
        );

        return ResponseEntity.ok(toWatchlistResponse(watchlistItem));
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<Void> removeFromWatchlist(@PathVariable Long userId, @PathVariable Long movieId) {
        authorizationService.verifyOwnership(userId);
        watchlistService.removeFromWatchlist(userId, movieId);
        return ResponseEntity.noContent().build();
    }
}
