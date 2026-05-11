package ro.anthony.mediarecommenderapp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import ro.anthony.mediarecommenderapp.model.WatchlistStatus;

@Getter
@Setter
public class WatchlistStatusUpdateRequest {
    @NotNull
    private WatchlistStatus status;
}
