package devkor.com.teamcback.domain.ble.entity;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BLEstatus {
    VACANT(0, "여유"),
    AVAILABLE(1, "보통"),
    CROWDED(2, "포화"),
    FAILURE(3, "신호없음");

    private final int code;
    private final String label;
}
