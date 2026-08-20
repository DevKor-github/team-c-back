package devkor.com.teamcback.domain.notification.template;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainPushContentFactoryTest {

    @Test
    void placeBecameVacantJoinsBuildingAndPlaceWithSingleSpace() {
        PushContent content = DomainPushContentFactory.placeBecameVacant(
                " 신공학관 ",
                " 라운지 "
        );

        assertThat(content.title()).isEqualTo("기다리던 자리가 생겼어요!");
        assertThat(content.body()).isEqualTo("신공학관 라운지이 한산해요. 방문하기 전 현황을 확인해보세요.");
    }

    @Test
    void placeBecameVacantDoesNotIncludeNullWhenOneValueExists() {
        PushContent buildingOnly = DomainPushContentFactory.placeBecameVacant("신공학관", null);
        PushContent placeOnly = DomainPushContentFactory.placeBecameVacant(null, "라운지");

        assertThat(buildingOnly.body()).isEqualTo("신공학관이 한산해요. 방문하기 전 현황을 확인해보세요.");
        assertThat(placeOnly.body()).isEqualTo("라운지이 한산해요. 방문하기 전 현황을 확인해보세요.");
        assertThat(buildingOnly.body()).doesNotContain("null");
        assertThat(placeOnly.body()).doesNotContain("null");
    }

    @Test
    void placeBecameVacantUsesFallbackWhenBothValuesAreBlank() {
        PushContent content = DomainPushContentFactory.placeBecameVacant(" ", null);

        assertThat(content.body()).isEqualTo("즐겨찾기한 공간이 한산해요. 방문하기 전 현황을 확인해보세요.");
    }

    @Test
    void characterUnlockedUsesFallbackWhenNameIsNullOrBlank() {
        PushContent nullName = DomainPushContentFactory.characterUnlocked(null);
        PushContent blankName = DomainPushContentFactory.characterUnlocked(" ");

        assertThat(nullName.title()).isEqualTo("새 캐릭터가 기다리고 있어요!");
        assertThat(nullName.body()).isEqualTo("새로운 캐릭터을 만나러 가볼까요?");
        assertThat(blankName.body()).isEqualTo("새로운 캐릭터을 만나러 가볼까요?");
    }

    @Test
    void reportResolvedCreatesConfiguredTitleAndBody() {
        PushContent content = DomainPushContentFactory.reportResolved();

        assertThat(content.title()).isEqualTo("신고 처리 결과를 확인해주세요.");
        assertThat(content.body()).isEqualTo("접수한 신고의 처리가 완료되었습니다. 고대로에서 결과를 확인해주세요.");
    }
    @Test
    void surveyContentsAreConfiguredExactly() {
        assertThat(DomainPushContentFactory.surveyStarted())
                .isEqualTo(new PushContent(
                        "고대로를 함께 만들어주세요!",
                        "잠깐의 설문으로 고대로를 더 편리하게 만들어주세요."
                ));
        assertThat(DomainPushContentFactory.surveyDMinus3())
                .isEqualTo(new PushContent(
                        "단 3초! 고대로의 개선을 위해 도와주세요",
                        "잠깐의 설문으로 고대로를 더 편리하게 만들어주세요."
                ));
        assertThat(DomainPushContentFactory.surveyDeadline(100))
                .isEqualTo(new PushContent(
                        "설문이 오늘 마감돼요!",
                        "설문에 참여하면 100 포인트를 받을 수 있어요.(5초 소요)"
                ));
        assertThat(DomainPushContentFactory.surveyRemindAfterLater(100))
                .isEqualTo(new PushContent(
                        "잠깐, 설문을 잊지 않으셨나요?",
                        "지금 투표에 참여하고 100포인트를 받아보세요.(5초 소요)"
                ));
    }
}
