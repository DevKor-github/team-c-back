package devkor.com.teamcback.domain.chatbot.dto;

import devkor.com.teamcback.domain.chatbot.tool.dto.ToolLocationType;

/** Safe semantic view of a location candidate. Backend identifiers are intentionally absent. */
public record LocationCandidateView(int index, String name, ToolLocationType locationType,
                                    String buildingName, String floor, String placeType) implements CandidateView {
    public LocationCandidateView {
        name = name == null ? "" : name;
        buildingName = buildingName == null ? "" : buildingName;
        floor = floor == null ? "" : floor;
        placeType = placeType == null ? "" : placeType;
    }
}
