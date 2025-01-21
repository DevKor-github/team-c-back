package devkor.com.teamcback.global.exception.controller;

import devkor.com.teamcback.global.response.CommonResponse;
import devkor.com.teamcback.global.response.ResultCode;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomErrorController implements ErrorController {
    @RequestMapping("/error")
    public ResponseEntity<CommonResponse<String>> handleError() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new CommonResponse<>(ResultCode.UNSUPPORTED_REQUEST));
    }
}
