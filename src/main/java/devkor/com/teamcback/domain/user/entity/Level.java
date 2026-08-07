package devkor.com.teamcback.domain.user.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Level {
    LEVEL1(0, 1, ProfileImage.getUrlByLevel(1)) { // 0~4점
        @Override
        public Level getNextLevel() {
            return LEVEL2;
        }
    },
    LEVEL2(5, 2, ProfileImage.getUrlByLevel(2)) { // 5~19점
        @Override
        public Level getNextLevel() {
            return LEVEL3;
        }
    },
    LEVEL3(20, 3, ProfileImage.getUrlByLevel(3)) { //20~39점
        @Override
        public Level getNextLevel() {
            return LEVEL4;
        }
    },
    LEVEL4(40, 4, ProfileImage.getUrlByLevel(4)) { //40~59점
        @Override
        public Level getNextLevel() {
            return LEVEL5;
        }
    },
    LEVEL5(60, 5, ProfileImage.getUrlByLevel(5)) { //60점 이상
        @Override
        public Level getNextLevel() {
            return null;
        }
    }, ;
    private final int minScore;
    private final int levelNumber;
    private final String profileImage;

    public abstract Level getNextLevel();
}