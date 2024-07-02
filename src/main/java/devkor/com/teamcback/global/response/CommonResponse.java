package devkor.com.teamcback.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "status 포함 common response")
@Getter
@AllArgsConstructor
public class CommonResponse<T> {
    @Schema(description = "http Status code")
    private final Integer statusCode;
    @Schema(description = "http status 메시지")
    private final String message;

    @Schema(description = "데이터")
    @JsonInclude(Include.NON_EMPTY)
    private T data;

    public CommonResponse(ResultCode resultCode) {
        this.statusCode = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    public CommonResponse(ResultCode resultCode, T data) {
        this.statusCode = resultCode.getCode();
        this.message = resultCode.getMessage();
        this.data = data;
    }

    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(ResultCode.SUCCESS, data);
    }

}
