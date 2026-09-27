package devkor.com.teamcback.domain.chatbot.service;

import devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation;
import devkor.com.teamcback.domain.chatbot.dto.PendingLocationRef;
import devkor.com.teamcback.domain.chatbot.dto.SearchResolutionTrace;
import devkor.com.teamcback.domain.chatbot.dto.RouteExecutionTrace;
import devkor.com.teamcback.domain.chatbot.tool.dto.RouteEndpointType;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindRouteToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindRouteToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.RouteCondition;
import devkor.com.teamcback.domain.chatbot.tool.dto.RouteEndpoint;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Collections;
import java.util.Map;

/** Request-local Tool execution data. Each LLM invocation owns one instance. */
public final class ResolvedLocationCollector {
    private final Map<String, ResolvedLocation> locations = Collections.synchronizedMap(new LinkedHashMap<>());
    private final List<SearchResolutionTrace> searchResolutions = Collections.synchronizedList(new ArrayList<>());
    private volatile RouteExecutionTrace routeExecution;

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

    public boolean hasTextRouteIntent() {
        synchronized (searchResolutions) {
            return searchResolutions.stream().anyMatch(trace -> trace.intent()
                    == devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusIntent.TEXT_ROUTE);
        }
    }

    public boolean hasRouteExecution() {
        return routeExecution != null;
    }

    public RouteExecutionTrace routeExecution() {
        return routeExecution;
    }

    public boolean hasRecordedToolActivity() {
        synchronized (searchResolutions) {
            return !searchResolutions.isEmpty() || routeExecution != null;
        }
    }

    /** Builds a route request only from unique current-request search results. */
    public synchronized FindRouteToolRequest currentTextRouteRequest() {
        if (!hasTextRouteIntent()) {
            return null;
        }
        ResolvedLocation start = unique(devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation.EndpointRole.START,
                devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation.RouteIntent.TEXT_ROUTE);
        ResolvedLocation end = unique(devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation.EndpointRole.END,
                devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation.RouteIntent.TEXT_ROUTE);
        if (!validEndpoint(start) || !validEndpoint(end)
                || (start.type() == end.type() && start.id().equals(end.id()))) {
            return null;
        }
        java.util.LinkedHashSet<RouteCondition> conditions = new java.util.LinkedHashSet<>();
        conditions.addAll(start.conditions());
        conditions.addAll(end.conditions());
        return new FindRouteToolRequest(
                new RouteEndpoint(start.type(), start.id(), null, null),
                new RouteEndpoint(end.type(), end.id(), null, null),
                conditions.stream().toList());
    }

    public synchronized void recordRouteExecution(FindRouteToolRequest request, FindRouteToolResult result) {
        FindRouteToolRequest expected = currentTextRouteRequest();
        if (expected == null || request == null || result == null || !sameEndpoint(expected.start(), request.start())
                || !sameEndpoint(expected.end(), request.end())) {
            return;
        }
        ResolvedLocation start = unique(ResolvedLocation.EndpointRole.START, ResolvedLocation.RouteIntent.TEXT_ROUTE);
        ResolvedLocation end = unique(ResolvedLocation.EndpointRole.END, ResolvedLocation.RouteIntent.TEXT_ROUTE);
        boolean successful = result.error() == null && result.route() != null;
        routeExecution = new RouteExecutionTrace(start, end, expected.conditions(), result.route(), successful);
    }

    public List<SearchResolutionTrace> searchResolutionSnapshot() {
        synchronized (searchResolutions) {
            return List.copyOf(searchResolutions);
        }
    }

    private ResolvedLocation unique(ResolvedLocation.EndpointRole role, ResolvedLocation.RouteIntent intent) {
        List<ResolvedLocation> matches;
        synchronized (locations) {
            matches = locations.values().stream().filter(location -> location.role() == role
                    && location.intent() == intent).toList();
        }
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private boolean validEndpoint(ResolvedLocation location) {
        return location != null && location.id() != null && location.id() > 0
                && (location.type() == RouteEndpointType.BUILDING || location.type() == RouteEndpointType.PLACE);
    }

    private boolean sameEndpoint(RouteEndpoint expected, RouteEndpoint actual) {
        return expected != null && actual != null && expected.type() == actual.type()
                && java.util.Objects.equals(expected.locationId(), actual.locationId());
    }
}
