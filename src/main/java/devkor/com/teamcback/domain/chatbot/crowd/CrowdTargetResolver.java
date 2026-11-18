package devkor.com.teamcback.domain.chatbot.crowd;

import devkor.com.teamcback.domain.ble.repository.BLEDeviceRepository;
import devkor.com.teamcback.domain.chatbot.search.ChatbotSearchCandidate;
import devkor.com.teamcback.domain.chatbot.tool.dto.ToolLocationType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Resolves crowd targets only from places backed by an actual BLE device. */
@Component
public class CrowdTargetResolver {
    private final BLEDeviceRepository bleDeviceRepository;

    public CrowdTargetResolver(BLEDeviceRepository bleDeviceRepository) {
        this.bleDeviceRepository = bleDeviceRepository;
    }

    @Transactional(readOnly = true)
    public Resolution resolve(String query, List<ChatbotSearchCandidate> locations) {
        String normalized = normalize(query);
        if (normalized.isBlank()) {
            return Resolution.notFound();
        }
        List<CrowdPlaceCandidate> all = bleDeviceRepository.findAllChatbotCrowdPlaces();
        List<CrowdPlaceCandidate> direct = all.stream()
                .filter(candidate -> matches(query, candidate, true))
                .toList();
        if (!direct.isEmpty()) {
            return Resolution.of(dedupe(direct));
        }
        ChatbotSearchCandidate building = uniqueBuilding(locations);
        if (building == null) {
            return Resolution.notFound();
        }
        return byBuilding(building.locationId());
    }

    @Transactional(readOnly = true)
    public Resolution byBuilding(Long buildingId) {
        if (buildingId == null || buildingId <= 0) {
            return Resolution.notFound();
        }
        return Resolution.of(dedupe(bleDeviceRepository.findChatbotCrowdPlacesByBuildingId(buildingId)));
    }

    @Transactional(readOnly = true)
    public Resolution resolveWithinBuilding(String query, Long buildingId) {
        String normalized = normalize(query);
        if (normalized.isBlank() || buildingId == null) {
            return Resolution.notFound();
        }
        List<CrowdPlaceCandidate> candidates = bleDeviceRepository.findChatbotCrowdPlacesByBuildingId(buildingId);
        List<CrowdPlaceCandidate> matched = candidates.stream()
                .filter(candidate -> matches(query, candidate, false))
                .toList();
        return Resolution.of(dedupe(matched));
    }

    private boolean matches(String query, CrowdPlaceCandidate candidate, boolean allowBuildingMatch) {
        String normalizedQuery = normalize(query);
        String queryWords = normalizeWords(query);
        String device = normalize(candidate.deviceName());
        String place = normalize(candidate.placeName());
        String building = normalize(candidate.buildingName());
        String buildingAndPlace = normalize(building + place);
        String deviceWords = normalizeWords(candidate.deviceName());
        String buildingAndPlaceWords = normalizeWords(candidate.buildingName())
                + " " + normalizeWords(candidate.placeName());
        return normalizedQuery.equals(device)
                || normalizedQuery.equals(buildingAndPlace)
                || normalizedQuery.equals(place)
                || (!device.isBlank() && device.contains(normalizedQuery))
                || containsAllTokens(queryWords, deviceWords)
                || containsAllTokens(queryWords, buildingAndPlaceWords)
                || (allowBuildingMatch && !building.isBlank() && normalizedQuery.equals(building));
    }

    private boolean containsAllTokens(String query, String candidate) {
        if (query.isBlank() || candidate.isBlank()) {
            return false;
        }
        for (String token : query.split(" ")) {
            if (!candidate.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private ChatbotSearchCandidate uniqueBuilding(List<ChatbotSearchCandidate> locations) {
        if (locations == null) {
            return null;
        }
        List<ChatbotSearchCandidate> buildings = locations.stream()
                .filter(candidate -> candidate.locationType() == ToolLocationType.BUILDING)
                .toList();
        if (buildings.size() != 1) {
            return null;
        }
        return buildings.get(0);
    }

    private List<CrowdPlaceCandidate> dedupe(List<CrowdPlaceCandidate> candidates) {
        Map<Long, CrowdPlaceCandidate> unique = new LinkedHashMap<>();
        for (CrowdPlaceCandidate candidate : candidates) {
            if (candidate != null && candidate.placeId() != null) {
                unique.putIfAbsent(candidate.placeId(), candidate);
            }
        }
        return List.copyOf(unique.values());
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "")
                .replace("(", "").replace(")", "")
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeWords(String value) {
        return value == null ? "" : value.replace("(", "").replace(")", "")
                .toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    public record Resolution(Status status, List<CrowdPlaceCandidate> candidates) {
        public Resolution {
            status = status == null ? Status.NOT_FOUND : status;
            candidates = candidates == null ? List.of() : List.copyOf(candidates);
        }

        public static Resolution of(List<CrowdPlaceCandidate> candidates) {
            if (candidates == null || candidates.isEmpty()) return notFound();
            return new Resolution(candidates.size() == 1 ? Status.UNIQUE : Status.AMBIGUOUS, candidates);
        }

        public static Resolution notFound() {
            return new Resolution(Status.NOT_FOUND, List.of());
        }

        public static Resolution ofForCompatibility(List<CrowdPlaceCandidate> candidates) {
            return of(candidates);
        }

        public static Resolution notFoundForCompatibility() {
            return notFound();
        }

        public enum Status { UNIQUE, AMBIGUOUS, NOT_FOUND }
    }
}
