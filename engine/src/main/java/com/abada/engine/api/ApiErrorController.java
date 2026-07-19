package com.abada.engine.api;

import com.abada.engine.dto.ErrorResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiErrorController implements org.springframework.boot.web.servlet.error.ErrorController {
    @RequestMapping("/error")
    public ResponseEntity<ErrorResponse> error(HttpServletRequest request) {
        Object statusValue = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int value = statusValue instanceof Integer integer ? integer : HttpStatus.INTERNAL_SERVER_ERROR.value();
        HttpStatus status = HttpStatus.resolve(value);
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        String path = String.valueOf(request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
        ApiErrorCode code = status == HttpStatus.NOT_FOUND ? ApiErrorCode.RESOURCE_NOT_FOUND
                : status.is4xxClientError() ? ApiErrorCode.INVALID_REQUEST : ApiErrorCode.INTERNAL_ERROR;
        String message = status == HttpStatus.NOT_FOUND ? "API resource not found" : status.getReasonPhrase();
        return ResponseEntity.status(status).body(ApiErrors.response(status, code, message, path,
                java.util.Map.of()));
    }
}
