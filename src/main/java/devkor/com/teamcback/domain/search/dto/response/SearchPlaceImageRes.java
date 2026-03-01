package devkor.com.teamcback.domain.search.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SearchPlaceImageRes {
    private Long imageId;
    private String image;

    public SearchPlaceImageRes(Long imageId, String image) {
        this.imageId = imageId;
        this.image = image;
    }
}
