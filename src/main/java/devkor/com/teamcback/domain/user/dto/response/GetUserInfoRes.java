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
    @Schema(description = "provider", example = "NAVER")
    private Provider provider;
    @Schema(description = "role", example = "USER")
    private Role role;
    @Schema(description = "level", example = "1")
    private int level;
    @Schema(description = "score", example = "15")
    private Long score;
    @Schema(description = "remainScoreToNextLevel", example = "10")
    private Long remainScoreToNextLevel;
    @Schema(description = "percent", example = "75")
    private int percent;
    @Schema(description = "categoryCount", example = "2")
    private Long categoryCount;
    //TODO: 이웃 수, 게시물 수 정보 추가하기

    public GetUserInfoRes(User user, Long categoryCount, int level, Long remainScoreToNextLevel, int percent) {
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.provider = user.getProvider();
        this.role = user.getRole();
        this.level = level;
        this.score = user.getScore();
        this.remainScoreToNextLevel = remainScoreToNextLevel;
        this.percent = percent;
        this.categoryCount = categoryCount;
    }
}
