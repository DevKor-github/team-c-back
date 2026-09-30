package devkor.com.teamcback.domain.chatbot.search;

import devkor.com.teamcback.domain.place.entity.PlaceType;

/** Minimal place projection used only by the chatbot resolver. */
public record ChatbotPlaceCandidate(Long id, String name, Long buildingId, String buildingName,
                                    Double floor, PlaceType placeType, String detail, String nickname) {
}
