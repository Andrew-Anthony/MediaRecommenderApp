package ro.anthony.mediarecommenderapp.dto.tmdb;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TmdbCastMember {
    private Integer id;
    private String name;
    private String character;
    private Integer order;
}
