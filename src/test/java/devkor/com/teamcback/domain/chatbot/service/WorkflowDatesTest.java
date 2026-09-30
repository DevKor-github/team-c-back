package devkor.com.teamcback.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class WorkflowDatesTest {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Test
    void parsesYearlessKoreanDateInSeoulCurrentYear() {
        LocalDate expected = LocalDate.of(LocalDate.now(SEOUL).getYear(), 9, 30);

        assertThat(WorkflowDates.menuDate("9월 30일")).isEqualTo(expected);
        assertThat(WorkflowDates.menuDate("9/30")).isEqualTo(expected);
    }

    @Test
    void rejectsInvalidYearlessDate() {
        assertThatThrownBy(() -> WorkflowDates.menuDate("2월 30일"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
