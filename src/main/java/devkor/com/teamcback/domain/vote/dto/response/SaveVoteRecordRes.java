package devkor.com.teamcback.domain.vote.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "투표 내용 저장 완료")
@JsonIgnoreProperties
public class SaveVoteRecordRes {
}
