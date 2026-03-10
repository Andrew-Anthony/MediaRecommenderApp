package ro.anthony.mediarecommenderapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.anthony.mediarecommenderapp.model.InteractionType;
import ro.anthony.mediarecommenderapp.model.MediaInteraction;

import java.util.List;

public interface MediaInteractionRepository extends JpaRepository<MediaInteraction, Long> {
    List<MediaInteraction> findByUserId(Long userId);
    List<MediaInteraction> findByUserIdAndInteractionType(Long userId, InteractionType interactionType);
    List<MediaInteraction> findByMovieId(Long movieId);
}
