package devkor.com.teamcback.domain.chatbot.dto;

/** Candidate fields safe for semantic selection; database identifiers are deliberately absent. */
public interface CandidateView {
    int index();

    String name();
}
