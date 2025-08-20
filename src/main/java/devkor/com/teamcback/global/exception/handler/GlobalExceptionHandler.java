package devkor.com.teamcback.global.exception.handler;

import devkor.com.teamcback.global.exception.exception.AdminException;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.CommonResponse;
import devkor.com.teamcback.global.response.ResultCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<CommonResponse<Void>> handleException(GlobalException e) {
        log.warn("⚠️GlobalException 발생: code={}, status={}, message={}",
            e.getResultCode().getCode(),
            e.getResultCode().getStatus(),
            e.getResultCode().getMessage()
        );
        return ResponseEntity.status(e.getResultCode().getStatus())
            .body(new CommonResponse<>(e.getResultCode()));
    }

    @ExceptionHandler(AdminException.class)
    public ResponseEntity<CommonResponse<String>> handleException(AdminException e) {
        return ResponseEntity.status(e.getResultCode().getStatus())
            .body(new CommonResponse<>(e.getResultCode(), e.getAdminMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<String>> handleValidationError(
        MethodArgumentNotValidException e) { // Validation 예외를 잡아줌
        BindingResult bindingResult = e.getBindingResult();

        StringBuilder sb = new StringBuilder();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            sb.append("[");
            sb.append(fieldError.getField());
            sb.append("](은)는 ");
            sb.append(fieldError.getDefaultMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new CommonResponse<>(ResultCode.INVALID_INPUT, sb.toString()));
    }

    @ExceptionHandler({SQLException.class, DataAccessException.class, ConstraintViolationException.class})
    public ResponseEntity<CommonResponse<String>> handleDBException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new CommonResponse<>(ResultCode.DB_ERROR, ex.getMessage()));
    }
}
