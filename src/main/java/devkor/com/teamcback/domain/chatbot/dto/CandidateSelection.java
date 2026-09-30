package devkor.com.teamcback.domain.chatbot.dto;

/** Provider-neutral semantic selection of one backend-owned candidate. */
public interface CandidateSelection {
    Status status();

    Integer candidateIndex();

    enum Status {
        SELECTED,
        AMBIGUOUS,
        NONE
    }
}
