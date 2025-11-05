package com.evdealer.evdealermanagement.exceptions;

import com.evdealer.evdealermanagement.dto.account.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidation(MethodArgumentNotValidException ex) {

        // Tạo đối tượng chứa tất cả các lỗi chi tiết (có thể đặt vào trường 'details' của ApiResponse)
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            // Cần kiểm tra nếu lỗi không phải FieldError (ví dụ lỗi ở cấp độ đối tượng)
            if (error instanceof FieldError) {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            } else {
                errors.put(error.getObjectName(), error.getDefaultMessage());
            }
        });

        // Trả về ApiResponse tiêu chuẩn
        ApiResponse apiResponse = new ApiResponse();
        // Sử dụng một mã lỗi chung cho lỗi Validation
        apiResponse.setCode(ErrorCode.INVALID_INPUT.getCode());
        // Thông báo có thể nói rõ là lỗi validation
        apiResponse.setMessage("Validation failed");
        apiResponse.setResult(errors); // Đính kèm chi tiết lỗi

        return ResponseEntity
                .badRequest() // HTTP Status 400
                .body(apiResponse);
    }
    /**
     * Handle custom AppException
     */
    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ApiResponse> handleAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        logger.error("AppException: {} - {}", errorCode.getCode(), errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getHttpStatus())  // ← Sửa: Dùng httpStatus thay vì badRequest()
                .body(apiResponse);
    }

    /**
     * Handle Access Denied (403) - Từ Spring Security
     */
    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDeniedException(AccessDeniedException exception) {
        ErrorCode errorCode = ErrorCode.FORBIDDEN;
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        logger.warn("Access denied: {}", exception.getMessage());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(apiResponse);
    }

    /**
     * Handle NullPointerException
     */
    @ExceptionHandler(value = NullPointerException.class)
    public ResponseEntity<ApiResponse> handleNullPointerException(NullPointerException exception) {
        logger.error("NullPointerException: ", exception);

        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage("A required value was missing");

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(apiResponse);
    }

    /**
     * Handle IllegalArgumentException
     */
    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(exception.getMessage() != null ? exception.getMessage() : errorCode.getMessage());

        logger.warn("Illegal argument: {}", exception.getMessage());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(apiResponse);
    }

    /**
     * Handle all other unhandled exceptions
     */
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiResponse> handleGenericException(Exception exception) {
        logger.error("Unhandled exception: ", exception);

        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(apiResponse);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse> handleResponseStatusException(ResponseStatusException exception) {
        logger.warn("ResponseStatusException: {}", exception.getReason());

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setCode(exception.getStatusCode().value());
        apiResponse.setMessage(exception.getReason());

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(apiResponse);
    }

}