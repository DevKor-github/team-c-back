package devkor.com.teamcback.domain.chatbot.search;

import devkor.com.teamcback.domain.building.repository.BuildingNicknameRepository;
import devkor.com.teamcback.domain.chatbot.tool.dto.ToolLocationType;
import devkor.com.teamcback.domain.place.repository.PlaceNicknameRepository;
import devkor.com.teamcback.domain.search.util.HangeulUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lightweight, non-personalized location resolver for searchCampus. */
@Service
public class ChatbotCampusSearchService {
    private static final int INTERNAL_CANDIDATE_LIMIT = 50;

    private final BuildingNicknameRepository buildingNicknameRepository;
    private final PlaceNicknameRepository placeNicknameRepository;

    public ChatbotCampusSearchService(BuildingNicknameRepository buildingNicknameRepository,
                                      PlaceNicknameRepository placeNicknameRepository) {
        this.buildingNicknameRepository = buildingNicknameRepository;
        this.placeNicknameRepository = placeNicknameRepository;
    }

    @Transactional(readOnly = true)
    public List<ChatbotSearchCandidate> search(String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }

        Pageable limit = PageRequest.of(0, INTERNAL_CANDIDATE_LIMIT);
        List<ChatbotSearchCandidate> candidates = new ArrayList<>();
        String compact = compact(trimmed);
        addBuildingCandidates(candidates, findBuildings(compact, limit), 300);
        addPlaceCandidates(candidates, findPlaces(compact, null, limit), 290);

        String[] words = trimmed.split("\\s+");
        if (words.length > 1) {
            addCompositeCandidates(candidates, words[0], compact.substring(compact(words[0]).length()), 200, limit);
            String last = words[words.length - 1];
            String lastCompact = compact(last);
            String beforeLast = compact.substring(0, Math.max(0, compact.length() - lastCompact.length()));
            addCompositeCandidates(candidates, last, beforeLast, 190, limit);
        }

        Map<String, ChatbotSearchCandidate> unique = new LinkedHashMap<>();
        for (ChatbotSearchCandidate candidate : candidates) {
            String key = candidate.locationType() + ":" + candidate.locationId();
            ChatbotSearchCandidate previous = unique.get(key);
            if (previous == null || candidate.sourcePriority() > previous.sourcePriority()) {
                unique.put(key, candidate);
            }
        }
        List<ChatbotSearchCandidate> ranked = unique.values().stream()
                .sorted(java.util.Comparator.comparingInt(ChatbotSearchCandidate::sourcePriority).reversed()
                        .thenComparing(ChatbotSearchCandidate::name, java.util.Comparator.nullsLast(String::compareTo)))
                .toList();
        List<ChatbotSearchCandidate> strong = ranked.stream()
                .filter(candidate -> stronglyMatchesWholeQuery(trimmed, candidate))
                .toList();
        return strong.isEmpty() ? ranked : strong;
    }

    private void addCompositeCandidates(List<ChatbotSearchCandidate> target, String buildingWord,
                                        String placeWord, int priority, Pageable limit) {
        if (placeWord == null || placeWord.isBlank()) {
            return;
        }
        List<ChatbotBuildingCandidate> buildings = findBuildings(compact(buildingWord), limit);
        List<Long> buildingIds = buildings.stream().map(ChatbotBuildingCandidate::id)
                .filter(Objects::nonNull).distinct().toList();
        if (buildingIds.isEmpty()) {
            return;
        }
        addBuildingCandidates(target, buildings, priority, true);
        addPlaceCandidates(target, findPlaces(compact(placeWord), buildingIds, limit), priority - 1, true);
    }

    private List<ChatbotBuildingCandidate> findBuildings(String word, Pageable limit) {
        String jaso = HangeulUtils.decomposeHangulString(word);
        List<ChatbotBuildingCandidate> result = new ArrayList<>(
                buildingNicknameRepository.findChatbotByJaso(jaso, limit));
        if (HangeulUtils.isConsonantOnly(word)) {
            result.addAll(buildingNicknameRepository.findChatbotByChosung(
                    HangeulUtils.extractChosung(word), limit));
        }
        return result;
    }

    private List<ChatbotPlaceCandidate> findPlaces(String word, List<Long> buildingIds, Pageable limit) {
        String jaso = HangeulUtils.decomposeHangulString(word);
        List<ChatbotPlaceCandidate> result = new ArrayList<>();
        if (buildingIds == null) {
            result.addAll(placeNicknameRepository.findChatbotByJaso(jaso, limit));
            if (HangeulUtils.isConsonantOnly(word)) {
                result.addAll(placeNicknameRepository.findChatbotByChosung(
                        HangeulUtils.extractChosung(word), limit));
            }
        } else {
            result.addAll(placeNicknameRepository.findChatbotByJasoAndBuildingIds(jaso, buildingIds, limit));
            if (HangeulUtils.isConsonantOnly(word)) {
                result.addAll(placeNicknameRepository.findChatbotByChosungAndBuildingIds(
                        HangeulUtils.extractChosung(word), buildingIds, limit));
            }
        }
        return result;
    }

    private void addBuildingCandidates(List<ChatbotSearchCandidate> target,
                                       List<ChatbotBuildingCandidate> source, int priority) {
        addBuildingCandidates(target, source, priority, false);
    }

    private void addBuildingCandidates(List<ChatbotSearchCandidate> target,
                                       List<ChatbotBuildingCandidate> source, int priority, boolean composite) {
        for (ChatbotBuildingCandidate item : source) {
            if (item.id() != null) {
                target.add(new ChatbotSearchCandidate(item.id(), ToolLocationType.BUILDING, item.name(),
                        item.id(), item.name(), null, null, null, priority, item.nickname(), composite));
            }
        }
    }

    private void addPlaceCandidates(List<ChatbotSearchCandidate> target,
                                    List<ChatbotPlaceCandidate> source, int priority) {
        addPlaceCandidates(target, source, priority, false);
    }

    private void addPlaceCandidates(List<ChatbotSearchCandidate> target,
                                    List<ChatbotPlaceCandidate> source, int priority, boolean composite) {
        for (ChatbotPlaceCandidate item : source) {
            if (item.id() != null) {
                target.add(new ChatbotSearchCandidate(item.id(), ToolLocationType.PLACE, item.name(),
                        item.buildingId(), item.buildingName(), item.floor(), item.placeType(), item.detail(), priority,
                        item.nickname(), composite));
            }
        }
    }

    private boolean stronglyMatchesWholeQuery(String query, ChatbotSearchCandidate candidate) {
        String normalizedQuery = normalize(query);
        if (candidate.locationType() == ToolLocationType.BUILDING) {
            return normalizedQuery.equals(normalize(candidate.name()))
                    || normalizedQuery.equals(normalize(candidate.matchedNickname()));
        }

        if (!candidate.compositeQuery()) {
            return normalizedQuery.equals(normalize(candidate.name()))
                    || normalizedQuery.equals(normalize(candidate.matchedNickname()));
        }

        String parentAndPlace = normalize((candidate.buildingName() == null ? "" : candidate.buildingName())
                + (candidate.name() == null ? "" : candidate.name()));
        String parentAndNickname = normalize((candidate.buildingName() == null ? "" : candidate.buildingName())
                + (candidate.matchedNickname() == null ? "" : candidate.matchedNickname()));
        return normalizedQuery.equals(parentAndPlace) || normalizedQuery.equals(parentAndNickname);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", "")
                .replace("(", "")
                .replace(")", "")
                .replace("[", "")
                .replace("]", "")
                .toLowerCase(Locale.ROOT);
    }

    private String compact(String value) {
        return value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
