package devkor.com.teamcback.domain.user.entity;

import devkor.com.teamcback.global.exception.GlobalException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static devkor.com.teamcback.global.response.ResultCode.INCORRECT_LEVEL;

@Component
public class ProfileImage {
    @Value("${profile.image.lv1-url}")
    private String lv1Url;

    @Value("${profile.image.lv2-url}")
    private String lv2Url;

    @Value("${profile.image.lv3-url}")
    private String lv3Url;

    @Value("${profile.image.lv4-url}")
    private String lv4Url;

    @Value("${profile.image.lv5-url}")
    private String lv5Url;

    private static ProfileImage profileImage;

    public ProfileImage() {
        profileImage = this;
    }

    public static String getUrlByLevel(int level) {
        return switch (level) {
            case 1 -> profileImage.lv1Url;
            case 2 -> profileImage.lv2Url;
            case 3 -> profileImage.lv3Url;
            case 4 -> profileImage.lv4Url;
            case 5 -> profileImage.lv5Url;
            default -> throw new GlobalException(INCORRECT_LEVEL);
        };
    }
}
