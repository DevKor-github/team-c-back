package devkor.com.teamcback.domain.chatbot.service;

import devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation;
import devkor.com.teamcback.domain.chatbot.dto.PendingLocationRef;
import devkor.com.teamcback.domain.chatbot.dto.SearchResolutionTrace;
import devkor.com.teamcback.domain.chatbot.tool.dto.RouteEndpointType;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Collections;
import java.util.Map;

/** Request-local Tool execution data. Each LLM invocation owns one instance. */
public final class ResolvedLocationCollector {
    private final Map<String, ResolvedLocation> locations = Collections.synchronizedMap(new LinkedHashMap<>());
    private final List<SearchResolutionTrace> searchResolutions = Collections.synchronizedList(new ArrayList<>());

    public void record(SearchCampusToolRequest request, SearchCampusToolResult result) {
        if (request == null || request.role() == null || request.intent() == null || result == null) {
            return;
        }
        List<PendingLocationRef> candidates = result.candidates() == null ? List.of() : result.candidates().stream()
                .filter(item -> item != null && item.locationId() != null && item.locationId() > 0
                        && item.locationType() != null && item.name() != null && !item.name().isBlank())
                .map(item -> {
                    try {
                        return new PendingLocationRef(RouteEndpointType.valueOf(item.locationType().name()),
                                item.locationId(), item.name());
                    } catch (IllegalArgumentException exception) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();
        searchResolutions.add(new SearchResolutionTrace(request.role(), request.intent(), request.query(),
                result.ambiguous(), candidates, request.conditions()));
        if (result.ambiguous() || result.error() != null
                || result.candidates() == null || result.candidates().size() != 1) {
            return;
        }
        var item = result.candidates().get(0);
        if (item == null || item.locationId() == null || item.locationId() <= 0 || item.locationType() == null
                || item.name() == null || item.name().isBlank()) {
            return;
        }
        RouteEndpointType type;
        try {
            type = RouteEndpointType.valueOf(item.locationType().name());
        } catch (IllegalArgumentException exception) {
            return;
        }
        ResolvedLocation location = new ResolvedLocation(
                request.role().toEndpointRole(), type, item.locationId(), item.name(),
                request.intent().toRouteIntent(), request.conditions());
        locations.put(request.role() + ":" + type + ":" + item.locationId(), location);
    }

    public List<ResolvedLocation> snapshot() {
        synchronized (locations) {
            return List.copyOf(new ArrayList<>(locations.values()));
        }
    }

    public boolean hasNavigateIntent() {
        synchronized (locations) {
            return locations.values().stream()
                    .anyMatch(location -> location.intent() == ResolvedLocation.RouteIntent.NAVIGATE_ROUTE);
        }
    }

    public List<SearchResolutionTrace> searchResolutionSnapshot() {
        synchronized (searchResolutions) {
            return List.copyOf(searchResolutions);
        }
    }
}
