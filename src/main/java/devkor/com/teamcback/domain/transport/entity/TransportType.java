package devkor.com.teamcback.domain.transport.entity;

import lombok.Getter;

@Getter
public enum TransportType {
    STAIR(15), ELEVATOR(5), ESCALATOR(8);

    private final int duration;

    TransportType(int duration) {
        this.duration = duration;
    }
}
