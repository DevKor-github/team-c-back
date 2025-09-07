package devkor.com.teamcback.domain.vote.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VoteStatus {
    OPEN("진행 중"),
    CLOSED("종료");

    private final String name;
}
