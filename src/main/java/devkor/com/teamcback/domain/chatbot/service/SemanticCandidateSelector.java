package devkor.com.teamcback.domain.chatbot.service;

import devkor.com.teamcback.domain.chatbot.dto.CandidateSelection;
import devkor.com.teamcback.domain.chatbot.dto.CandidateView;
import java.util.List;

/** Small boundary for selectors that interpret user language over backend-owned candidate indexes. */
public interface SemanticCandidateSelector<V extends CandidateView> {
    CandidateSelection select(String userMessage, List<V> candidates);
}
