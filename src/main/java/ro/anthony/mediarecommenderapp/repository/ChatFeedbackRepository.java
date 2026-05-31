package ro.anthony.mediarecommenderapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.anthony.mediarecommenderapp.model.ChatFeedback;

import java.util.List;
import java.util.Optional;

public interface ChatFeedbackRepository extends JpaRepository<ChatFeedback, Long> {
    List<ChatFeedback> findByUserId(Long userId);
    Optional<ChatFeedback> findByUserIdAndMovieId(Long userId, Long movieId);
    void deleteByUserIdAndMovieId(Long userId, Long movieId);
}
