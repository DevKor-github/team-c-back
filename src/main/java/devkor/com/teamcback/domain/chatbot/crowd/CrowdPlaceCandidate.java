package devkor.com.teamcback.domain.chatbot.crowd;

import devkor.com.teamcback.domain.place.entity.PlaceType;

/** Minimal projection of a place that has a real BLE crowd device. */
public record CrowdPlaceCandidate(Long placeId, String deviceName, String placeName,
                                  Double floor, PlaceType placeType, Long buildingId,
                                  String buildingName) {
}
