package devkor.com.teamcback.domain.chatbot.dto;

/** Structured, provider-neutral interpretation of a crowd place selection. */
public record CrowdCandidateSelection(Status status, Integer candidateIndex) {
    public CrowdCandidateSelection {
        status = status == null ? Status.NONE : status;
    }

    public static CrowdCandidateSelection none() {
        return new CrowdCandidateSelection(Status.NONE, null);
    }

    public enum Status {
        SELECTED,
        AMBIGUOUS,
        NONE
    }
}
