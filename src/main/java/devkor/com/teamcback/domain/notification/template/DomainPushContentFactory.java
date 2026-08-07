package devkor.com.teamcback.domain.notification.template;

public final class DomainPushContentFactory {

    private static final String VACANT_TITLE =
            "기다리던 자리가 생겼어요!";

    private static final String REPORT_RESOLVED_TITLE =
            "신고 처리 결과를 확인해주세요.";

    private static final String REPORT_RESOLVED_BODY =
            "접수한 신고의 처리가 완료되었습니다. 고대로에서 결과를 확인해주세요.";

    private static final String CHARACTER_UNLOCKED_TITLE =
            "새 캐릭터가 기다리고 있어요!";

    private DomainPushContentFactory() {
    }

    public static PushContent placeBecameVacant(
            String buildingName,
            String placeName
    ) {
        String location = joinNonBlank(buildingName, placeName);

        return new PushContent(
                VACANT_TITLE,
                location + "이 한산해요. 방문하기 전 현황을 확인해보세요."
        );
    }

    public static PushContent reportResolved() {
        return new PushContent(
                REPORT_RESOLVED_TITLE,
                REPORT_RESOLVED_BODY
        );
    }

    public static PushContent characterUnlocked(String characterName) {
        String name = isBlank(characterName)
                ? "새로운 캐릭터"
                : characterName.trim();

        return new PushContent(
                CHARACTER_UNLOCKED_TITLE,
                name + "을 만나러 가볼까요?"
        );
    }

    public static PushContent surveyStarted() {
        return new PushContent(
                "고대로를 함께 만들어주세요!",
                "잠깐의 설문으로 고대로를 더 편리하게 만들어주세요."
        );
    }

    public static PushContent surveyDMinus3() {
        return new PushContent(
                "단 3초! 고대로의 개선을 위해 도와주세요",
                "잠깐의 설문으로 고대로를 더 편리하게 만들어주세요."
        );
    }

    public static PushContent surveyDeadline(int rewardPoint) {
        return new PushContent(
                "설문이 오늘 마감돼요!",
                "설문에 참여하면 " + rewardPoint + " 포인트를 받을 수 있어요.(5초 소요)"
        );
    }

    public static PushContent surveyRemindAfterLater(int rewardPoint) {
        return new PushContent(
                "잠깐, 설문을 잊지 않으셨나요?",
                "지금 투표에 참여하고 " + rewardPoint + "포인트를 받아보세요.(5초 소요)"
        );
    }

    private static String joinNonBlank(
            String first,
            String second
    ) {
        String firstValue = isBlank(first) ? "" : first.trim();
        String secondValue = isBlank(second) ? "" : second.trim();

        String result = (firstValue + " " + secondValue).trim();

        return result.isBlank()
                ? "즐겨찾기한 공간"
                : result;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
