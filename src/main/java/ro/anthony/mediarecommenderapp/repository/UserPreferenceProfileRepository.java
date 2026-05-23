package ro.anthony.mediarecommenderapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ro.anthony.mediarecommenderapp.model.UserPreferenceProfile;

@Repository
public interface UserPreferenceProfileRepository extends JpaRepository<UserPreferenceProfile, Long> {
}
