package devkor.com.teamcback.domain.navigate.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
@NoArgsConstructor
public class GetRouteRes {
    private InnerRouteRes innerRoute1;
    private OuterRouteRes outerRoute;
    private InnerRouteRes innerRoute2;
}