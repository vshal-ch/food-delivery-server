package com.amt_softwares.unnamed.common.exception;

import com.amt_softwares.unnamed.common.api.ApiResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ApiResponse<Void>> handleError(HttpServletRequest request) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = resolveStatus(statusCode);
        String message = resolveMessage(status);

        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(message, List.of(message)));
    }

    private HttpStatus resolveStatus(Object statusCode) {
        if (statusCode instanceof Integer code) {
            return HttpStatus.resolve(code) == null ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.resolve(code);
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveMessage(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Route not found";
            case METHOD_NOT_ALLOWED -> "Method not supported";
            case FORBIDDEN -> "Forbidden";
            case UNAUTHORIZED -> "Unauthorized";
            default -> status.is5xxServerError() ? "Internal server error" : status.getReasonPhrase();
        };
    }
}
