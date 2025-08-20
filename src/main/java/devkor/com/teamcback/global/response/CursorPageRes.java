package devkor.com.teamcback.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "커서 기반 페이지네이션 응답")
public class CursorPageRes<T> {
	@Schema(description = "데이터 리스트")
	private List<T> content;

	@Schema(description = "다음 페이지 존재 여부", example = "true")
	private boolean hasNext;

	@Schema(description = "마지막 커서 ID", example = "99")
	private Long lastCursorId;

	public CursorPageRes(List<T> content, boolean hasNext, Long lastCursorId) {
		this.content = content;
		this.hasNext = hasNext;
		this.lastCursorId = lastCursorId;
	}
}
