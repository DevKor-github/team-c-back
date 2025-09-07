package devkor.com.teamcback.domain.vote.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "투표 종료 완료")
@JsonIgnoreProperties
public class ChangeVoteStatusRes {
}
