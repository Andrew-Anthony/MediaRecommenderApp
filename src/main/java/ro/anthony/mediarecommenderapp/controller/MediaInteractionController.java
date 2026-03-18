package ro.anthony.mediarecommenderapp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.anthony.mediarecommenderapp.dto.MediaInteractionRequest;
import ro.anthony.mediarecommenderapp.dto.MediaInteractionResponse;
import ro.anthony.mediarecommenderapp.model.InteractionType;
import ro.anthony.mediarecommenderapp.model.MediaInteraction;
import ro.anthony.mediarecommenderapp.service.MediaInteractionService;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/interactions")
@RequiredArgsConstructor
public class MediaInteractionController {
    private final MediaInteractionService mediaInteractionService;

    private MediaInteractionResponse toMediaInteractionResponse(MediaInteraction mediaInteraction) {
        MediaInteractionResponse mediaInteractionResponse = new MediaInteractionResponse();
        mediaInteractionResponse.setId(mediaInteraction.getId());
        mediaInteractionResponse.setUserId(mediaInteraction.getUser().getId());
        mediaInteractionResponse.setUsername(mediaInteraction.getUser().getUsername());
        mediaInteractionResponse.setMovieId(mediaInteraction.getMovie().getId());
        mediaInteractionResponse.setMovieTitle(mediaInteraction.getMovie().getTitle());
        mediaInteractionResponse.setInteractionType(mediaInteraction.getInteractionType());
        mediaInteractionResponse.setTimestamp(mediaInteraction.getTimestamp());
        return mediaInteractionResponse;
    }

    @PostMapping
    public ResponseEntity<MediaInteractionResponse> recordInteraction(@PathVariable Long userId,
                                                                      @Valid @RequestBody MediaInteractionRequest request) {
        MediaInteraction mediaInteraction = mediaInteractionService.recordInteraction(
                userId,
                request.getMovieId(),
                request.getInteractionType()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toMediaInteractionResponse(mediaInteraction));
    }

    @GetMapping
    public ResponseEntity<List<MediaInteractionResponse>> getInteractions(@PathVariable Long userId,
                                                                          @RequestParam(required = false) InteractionType interactionType) {
        List<MediaInteraction> mediaInteractionList;

        if(interactionType != null) {
            mediaInteractionList = mediaInteractionService.getUserInteractionsByType(userId, interactionType);
        } else {
            mediaInteractionList = mediaInteractionService.getUserInteractions(userId);
        }

        List<MediaInteractionResponse> mediaInteractionResponseList = mediaInteractionList.stream().map(this::toMediaInteractionResponse).toList();
        return ResponseEntity.ok(mediaInteractionResponseList);
    }
}
