package ro.anthony.mediarecommenderapp.dto.tmdb;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TmdbCredits {
    private List<TmdbCastMember> cast;
    private List<TmdbCrewMember> crew;
}
