package devkor.com.teamcback.domain.report.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReasonCategory {
    ABUSE_OR_DISCRIMINATION("욕설/비하"),
    DEFAMATION("명예훼손"),
    SPAM_OR_ADVERTISING("홍보/도배"),
    INAPPROPRIATE_CONTENT("음란/선정");

    private final String label;
}
