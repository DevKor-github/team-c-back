package devkor.com.teamcback.domain.chatbot.dto;

public record LocationCandidateSelection(Status status, Integer candidateIndex) implements CandidateSelection {
    public LocationCandidateSelection {
        status = status == null ? Status.NONE : status;
    }

    public static LocationCandidateSelection none() {
        return new LocationCandidateSelection(Status.NONE, null);
    }
}
