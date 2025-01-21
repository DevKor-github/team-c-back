package devkor.com.teamcback.global.exception.exception;

import devkor.com.teamcback.global.response.ResultCode;
import lombok.Getter;

@Getter
public class AdminException extends GlobalException {
    private String adminMessage;

    public AdminException(ResultCode resultCode, String adminMessage) {
        super(resultCode);
        this.adminMessage = adminMessage;
    }
}
