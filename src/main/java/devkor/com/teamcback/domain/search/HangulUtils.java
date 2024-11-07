package devkor.com.teamcback.domain.search;

import org.springframework.stereotype.Component;

@Component
public class HangulUtils {
    private static final char HANGUL_BASE = 0xAC00;
    private static final char[] CHOSUNG = {
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };
    private static final char[] JUNGSUNG = {
        'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ', 'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ'
    };
    private static final char[] JONGSUNG = {
        ' ', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ', 'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };
    private static final String CONSONANT_ONLY_REGEX = "^[ㄱ-ㅎ]+$";

    public boolean isConsonantOnly(String str) {
        return str.matches(CONSONANT_ONLY_REGEX);
    }

    // 초성 분리 메소드
    public String extractChosung(String s) {
        StringBuilder result = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= HANGUL_BASE && c <= (HANGUL_BASE + 11171)) {
                int baseCode = c - HANGUL_BASE;
                int chosungIdx = baseCode / (21 * 28);
                result.append(CHOSUNG[chosungIdx]);
            } else {
                // 한글이 아닌 문자는 그대로 추가
                result.append(c);
            }
        }
        return result.toString();
    }

    // 자소 분리 메소드
    public String decomposeHangulString(String s) {
        StringBuilder result = new StringBuilder();
        for (char c : s.toCharArray()) {
            String[] parts = decomposeHangul(c);
            for (String part : parts) {
                result.append(part);
            }
        }
        return result.toString();
    }

    public String[] decomposeHangul(char c) {
        if (c < HANGUL_BASE || c > HANGUL_BASE + 11171) {
            return new String[]{Character.toString(c)};
        }

        int baseCode = c - HANGUL_BASE;
        int chosungIdx = baseCode / (21 * 28);
        int jungsungIdx = (baseCode % (21 * 28)) / 28;
        int jongsungIdx = baseCode % 28;

        String jong = jongsungIdx != 0 ? Character.toString(JONGSUNG[jongsungIdx]) : "";
        String jungsung = decomposeJungsung(JUNGSUNG[jungsungIdx]);
        String processedJong = processJongsung(jong);

        return new String[]{Character.toString(CHOSUNG[chosungIdx]), jungsung, processedJong};
    }

    private String decomposeJungsung(char jungsung) {
        return switch (jungsung) {
            case 'ㅘ' -> "ㅗㅏ";
            case 'ㅙ' -> "ㅗㅐ";
            case 'ㅚ' -> "ㅗㅣ";
            case 'ㅝ' -> "ㅜㅓ";
            case 'ㅞ' -> "ㅜㅔ";
            case 'ㅟ' -> "ㅜㅣ";
            default -> Character.toString(jungsung);
        };
    }

    // 종성 처리
    private String processJongsung(String jong) {
        return switch (jong) {
            case "ㄵ" -> "ㄴㅈ";
            case "ㄶ" -> "ㄴㅎ";
            case "ㄳ" -> "ㄱㅅ";
            case "ㄺ" -> "ㄹㄱ";
            case "ㄻ" -> "ㄹㅁ";
            case "ㄼ" -> "ㄹㅂ";
            case "ㄽ" -> "ㄹㅅ";
            case "ㄾ" -> "ㄹㅌ";
            case "ㄿ" -> "ㄹㅍ";
            case "ㅀ" -> "ㄹㅎ";
            default -> jong;
        };
    }
}
