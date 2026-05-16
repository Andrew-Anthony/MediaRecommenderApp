package ro.anthony.mediarecommenderapp.dto.tmdb;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TmdbCrewMember {
    private Integer id;
    private String name;
    private String job;
    private String department;
}
