package ro.anthony.mediarecommenderapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ro.anthony.mediarecommenderapp.model.InteractionType;
import ro.anthony.mediarecommenderapp.model.MediaInteraction;
import ro.anthony.mediarecommenderapp.model.Movie;
import ro.anthony.mediarecommenderapp.model.User;
import ro.anthony.mediarecommenderapp.repository.MediaInteractionRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MediaInteractionService {
    private final MediaInteractionRepository mediaInteractionRepository;
    private final UserService userService;
    private final MovieService movieService;

    public List<MediaInteraction> getUserInteractions(Long userId) {
        return mediaInteractionRepository.findByUserId(userId);
    }

    public List<MediaInteraction> getUserInteractionsByType(Long userId, InteractionType interactionType) {
        return mediaInteractionRepository.findByUserIdAndInteractionType(userId, interactionType);
    }

    public MediaInteraction recordInteraction(Long userId, Long movieId, InteractionType interactionType) {
        User user = userService.getUserById(userId);
        Movie movie = movieService.getMovieById(movieId);

        MediaInteraction mediaInteraction = new MediaInteraction();
        mediaInteraction.setUser(user);
        mediaInteraction.setMovie(movie);
        mediaInteraction.setInteractionType(interactionType);

        return mediaInteractionRepository.save(mediaInteraction);
    }
}
