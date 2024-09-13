package devkor.com.teamcback.domain.bookmark.entity;

import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_user_bookmark_log")
@NoArgsConstructor
public class UserBookmarkLog {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private Long locationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationType locationType;

    @ManyToOne
    @JoinColumn(name = "userId")
    private User user;

    public UserBookmarkLog (Long locationId, LocationType locationType, User user) {
        this.locationId = locationId;
        this.locationType = locationType;
        this.user = user;
    }


}
