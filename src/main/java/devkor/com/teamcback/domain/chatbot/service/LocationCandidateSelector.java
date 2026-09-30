package devkor.com.teamcback.domain.chatbot.service;

import devkor.com.teamcback.domain.chatbot.dto.LocationCandidateSelection;
import devkor.com.teamcback.domain.chatbot.dto.LocationCandidateView;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Interprets a clarification reply using safe candidate views; it never receives backend IDs. */
@Component
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class LocationCandidateSelector implements SemanticCandidateSelector<LocationCandidateView> {
    private static final String PROMPT = """
            Select exactly one campus location from the candidates below.
            Return SELECTED only for one clear match, AMBIGUOUS when unclear, and NONE when no candidate matches.
            Return only a candidate index. Never invent IDs or call tools.
            Candidates:\n%s
            """;

    private final LlmGateway gateway;

    public LocationCandidateSelector(LlmGateway gateway) { this.gateway = gateway; }

    @Override
    public LocationCandidateSelection select(String userMessage, List<LocationCandidateView> candidates) {
        List<LocationCandidateView> safe = candidates == null ? List.of() : List.copyOf(candidates);
        if (safe.isEmpty()) return LocationCandidateSelection.none();
        String text = safe.stream().map(c -> "%d: %s%s".formatted(c.index(), c.name(),
                        c.buildingName().isBlank() ? "" : " (" + c.buildingName() + ")"))
                .reduce((a, b) -> a + "\n" + b).orElse("");
        LocationCandidateSelection selection = gateway.selectLocationCandidate(
                PROMPT.formatted(text), List.of(), userMessage, safe);
        return selection == null ? LocationCandidateSelection.none() : selection;
    }
}
