package ro.anthony.mediarecommenderapp.dto.chat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RerankItem {
    private Integer tmdbId;
    private Double score;
    private String reason;
}
