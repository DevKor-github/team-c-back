package devkor.com.teamcback.domain.user.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Level {
    LEVEL1(0, 1) { // 0~4점
        @Override
        public Level getNextLevel() {
            return LEVEL2;
        }
    },
    LEVEL2(5, 2) { // 5~19점
        @Override
        public Level getNextLevel() {
            return LEVEL3;
        }
    },
    LEVEL3(20, 3) { //20~39점
        @Override
        public Level getNextLevel() {
            return LEVEL4;
        }
    },
    LEVEL4(40, 4) { //40~59점
        @Override
        public Level getNextLevel() {
            return LEVEL5;
        }
    },
    LEVEL5(60, 5) { //60점 이상
        @Override
        public Level getNextLevel() {
            return null;
        }
    }, ;
    private final int minScore;
    private final int levelNumber;

    public abstract Level getNextLevel();

    // 엔티티 필드로 사용되는 enum이 Hibernate 메타데이터 빌드 시점에 초기화되므로,
    // ProfileImage 빈 생성 전에 접근하지 않도록 이미지 URL은 호출 시점에 조회한다.
    public String getProfileImage() {
        return ProfileImage.getUrlByLevel(levelNumber);
    }

    public static Level fromScore(long score) {
        // score >= minScore 인 경우 중 가장 높은 레벨 반환
        Level result = LEVEL1;
        for (Level level : values()) {
            if (score >= level.getMinScore() && level.getMinScore() >= result.getMinScore()) {
                result = level;
            }
        }
        return result;
    }
}
