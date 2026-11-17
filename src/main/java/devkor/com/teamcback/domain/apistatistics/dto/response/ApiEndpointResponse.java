package devkor.com.teamcback.domain.apistatistics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiEndpointResponse {
    private String endpoint;
    private String method;
}
