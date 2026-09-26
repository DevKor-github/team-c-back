package devkor.com.teamcback.domain.chatbot.tool.dto;

import devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation.EndpointRole;

public enum SearchCampusRole {
    START, END;

    public EndpointRole toEndpointRole() {
        return EndpointRole.valueOf(name());
    }
}
