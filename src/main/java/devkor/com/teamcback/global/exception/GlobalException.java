package devkor.com.teamcback.global.exception;

import devkor.com.teamcback.global.response.ResultCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GlobalException extends RuntimeException{
    private final ResultCode resultCode;
}
