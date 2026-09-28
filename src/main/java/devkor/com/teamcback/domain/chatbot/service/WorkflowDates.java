package devkor.com.teamcback.domain.chatbot.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

final class WorkflowDates {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private WorkflowDates() {}
    static LocalDate menuDate(String expression) {
        LocalDate today = LocalDate.now(SEOUL);
        if (expression == null || expression.isBlank() || expression.contains("오늘")) return today;
        if (expression.contains("내일")) return today.plusDays(1);
        String value = expression.trim().toLowerCase(Locale.ROOT);
        DayOfWeek day = switch (value.replace("요일", "")) {
            case "월", "mon", "monday" -> DayOfWeek.MONDAY; case "화", "tue", "tuesday" -> DayOfWeek.TUESDAY;
            case "수", "wed", "wednesday" -> DayOfWeek.WEDNESDAY; case "목", "thu", "thursday" -> DayOfWeek.THURSDAY;
            case "금", "fri", "friday" -> DayOfWeek.FRIDAY; case "토", "sat", "saturday" -> DayOfWeek.SATURDAY;
            case "일", "sun", "sunday" -> DayOfWeek.SUNDAY; default -> null;
        };
        if (day != null) return today.plusDays((day.getValue() - today.getDayOfWeek().getValue() + 7) % 7);
        for (DateTimeFormatter format : new DateTimeFormatter[]{DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("M월 d일"), DateTimeFormatter.ofPattern("M/d")}) {
            try { LocalDate parsed = LocalDate.parse(value, format); return parsed.withYear(today.getYear()); }
            catch (DateTimeParseException ignored) { }
        }
        throw new IllegalArgumentException("invalid date expression");
    }
    static devkor.com.teamcback.domain.common.entity.Weekday weekday(String expression) {
        if (expression == null || expression.isBlank() || expression.contains("오늘")) {
            return devkor.com.teamcback.domain.common.entity.Weekday.valueOf(LocalDate.now(SEOUL).getDayOfWeek().name().substring(0, 3));
        }
        String value = expression.trim().toLowerCase(Locale.ROOT).replace("요일", "");
        return switch (value) { case "월", "mon", "monday" -> devkor.com.teamcback.domain.common.entity.Weekday.MON;
            case "화", "tue", "tuesday" -> devkor.com.teamcback.domain.common.entity.Weekday.TUE;
            case "수", "wed", "wednesday" -> devkor.com.teamcback.domain.common.entity.Weekday.WED;
            case "목", "thu", "thursday" -> devkor.com.teamcback.domain.common.entity.Weekday.THU;
            case "금", "fri", "friday" -> devkor.com.teamcback.domain.common.entity.Weekday.FRI;
            case "토", "sat", "saturday" -> devkor.com.teamcback.domain.common.entity.Weekday.SAT;
            case "일", "sun", "sunday" -> devkor.com.teamcback.domain.common.entity.Weekday.SUN;
            default -> throw new IllegalArgumentException("invalid weekday expression"); };
    }
}
