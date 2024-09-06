package devkor.com.teamcback.domain.user.dto.response;

import devkor.com.teamcback.domain.user.entity.Provider;
import devkor.com.teamcback.domain.user.entity.Role;
import devkor.com.teamcback.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "마이페이지 정보")
public class GetUserInfoRes {
    @Schema(description = "username", example = "호랑이asdf")
    private String username;
    @Schema(description = "email", example = "abc1234@naver.com")
    private String email;
    @Schema(description = "profileUrl", example = "profileUrl")
    private String profileUrl;
    @Schema(description = "provider", example = "NAVER")
    private Provider provider;
    @Schema(description = "role", example = "USER")
    private Role role;
    @Schema(description = "level", example = "1")
    private int level;
    @Schema(description = "categoryCount", example = "2")
    private Long categoryCount;
    //TODO: 이웃 수, 게시물 수 정보 추가하기

    public GetUserInfoRes(User user, Long categoryCount) {

        this.username = user.getUsername();
        this.email = user.getEmail();
        this.profileUrl = user.getProfileUrl();
        this.provider = user.getProvider();
        this.role = user.getRole();
        if(user.getScore() < LevelConstraint.LEVEL_TWO_LIMIT) {
            this.level = 1;
        } else if (user.getScore() < LevelConstraint.LEVEL_THREE_LIMIT) {
            this.level = 2;
        } else {
            this.level = 3;
        }
        this.categoryCount = categoryCount;
    }

    public class LevelConstraint { // 굳이 상수 변수 필요 있을 지 생각해보기...
        private static final int LEVEL_TWO_LIMIT = 5;
        private static final int LEVEL_THREE_LIMIT = 15;
    }
}
