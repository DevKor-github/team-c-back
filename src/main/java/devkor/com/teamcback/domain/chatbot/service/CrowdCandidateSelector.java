package devkor.com.teamcback.domain.chatbot.service;

import devkor.com.teamcback.domain.chatbot.dto.CrowdCandidateSelection;
import devkor.com.teamcback.domain.chatbot.dto.CrowdCandidateView;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Interprets a PLACE_SELECTION reply without receiving tools or database identifiers. */
@Component
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class CrowdCandidateSelector implements SemanticCandidateSelector<CrowdCandidateView> {
    private static final String PROMPT = """
            Select a crowd location only from the supplied candidates.
            Return SELECTED only when the user's reply clearly identifies exactly one candidate.
            Return AMBIGUOUS when multiple candidates could match or when the user only confirms with words
            such as yes, okay, or 맞아. Return NONE when no candidate matches.
            Never invent an index. Return only the structured CrowdCandidateSelection format.
            Candidates are numbered by index:
            %s
            """;

    private final LlmGateway gateway;

    public CrowdCandidateSelector(LlmGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public CrowdCandidateSelection select(String userMessage, List<CrowdCandidateView> candidates) {
        List<CrowdCandidateView> safeCandidates = candidates == null ? List.of() : List.copyOf(candidates);
        if (safeCandidates.isEmpty()) {
            return CrowdCandidateSelection.none();
        }
        if (isBareConfirmation(userMessage)) {
            return new CrowdCandidateSelection(CrowdCandidateSelection.Status.AMBIGUOUS, null);
        }
        String candidateText = safeCandidates.stream()
                .map(candidate -> "%d: %s%s%s".formatted(candidate.index(), candidate.name(),
                        candidate.floor().isBlank() ? "" : " (floor=" + candidate.floor() + ")",
                        candidate.placeType().isBlank() ? "" : " (type=" + candidate.placeType() + ")"))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
        CrowdCandidateSelection selection = gateway.selectCrowdCandidate(
                PROMPT.formatted(candidateText), List.of(), userMessage, safeCandidates);
        return selection == null ? CrowdCandidateSelection.none() : selection;
    }

    private boolean isBareConfirmation(String userMessage) {
        if (userMessage == null) {
            return false;
        }
        return Set.of("\uC751", "\uB124", "\uC608", "\uB9DE\uC544", "\uB9DE\uC544\uC694",
                "\uADF8\uB798", "yes", "okay", "ok").contains(userMessage.trim().toLowerCase(Locale.ROOT));
    }
}
