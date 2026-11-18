package devkor.com.teamcback.domain.chatbot.tool.dto;

public record FindFacilitiesToolRequest(
        CampusFacilityType facilityType,
        Long buildingId,
        Integer floor,
        Integer limit
) {
}
