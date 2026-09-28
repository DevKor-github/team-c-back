package devkor.com.teamcback.domain.chatbot.dto;

/** Candidate information safe to expose to the semantic selector; it contains no database identifier. */
public record CrowdCandidateView(int index, String name, String floor, String placeType) implements CandidateView {
    public CrowdCandidateView {
        name = name == null ? "" : name;
        floor = floor == null ? "" : floor;
        placeType = placeType == null ? "" : placeType;
    }
}
