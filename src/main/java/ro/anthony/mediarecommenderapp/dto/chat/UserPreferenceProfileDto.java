package ro.anthony.mediarecommenderapp.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceProfileDto {
    private List<String> likes;
    private List<String> dislikes;
    private List<String> preferredThemes;

    public static UserPreferenceProfileDto empty() {
        return new UserPreferenceProfileDto(List.of(), List.of(), List.of());
    }

    public boolean isEmpty() {
        return (likes == null || likes.isEmpty())
                && (dislikes == null || dislikes.isEmpty())
                && (preferredThemes == null || preferredThemes.isEmpty());
    }
}
