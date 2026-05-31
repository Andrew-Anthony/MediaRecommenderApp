package ro.anthony.mediarecommenderapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_preference_profiles")
public class UserPreferenceProfile {

    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String likesJson;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String dislikesJson;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String preferredThemesJson;

    @Column(nullable = false)
    private Integer reviewCountAtGeneration;

    private Integer feedbackCountAtGeneration;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
