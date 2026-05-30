package evo.developers.com.cashcare.handler;

import evo.developers.com.cashcare.dto.response.ErrorResponse;
import evo.developers.com.cashcare.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final org.apache.commons.logging.Log log =
            org.apache.commons.logging.LogFactory.getLog(GlobalExceptionHandler.class);

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        ErrorResponse responseError = new ErrorResponse();
        responseError.setStatus(false);
        responseError.setMessage(ex.getMessage());

        return ResponseEntity
                .status(ex.getStatusHttpCode())
                .body(responseError);
    }

    @ExceptionHandler(org.springframework.web.client.ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleAiTimeout(
            org.springframework.web.client.ResourceAccessException ex
    ) {
        log.warn("AI request timed out: " + ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT)
                .body(ErrorResponse.error(
                        "AI слишком долго не отвечал. Попробуй ещё раз.",
                        java.util.List.of(ex.getMessage())
                ));
    }

    @ExceptionHandler(org.springframework.web.client.RestClientException.class)
    public ResponseEntity<ErrorResponse> handleAiRestError(
            org.springframework.web.client.RestClientException ex
    ) {
        log.warn("AI request failed: " + ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.error(
                        "Ошибка обращения к AI. Проверь, доступен ли OpenRouter.",
                        java.util.List.of(ex.getMessage())
                ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleGenericRuntime(RuntimeException ex) {
        log.error("Unhandled error: " + ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.error(
                        "Что-то пошло не так",
                        java.util.List.of(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage())
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleNotValidRequestDataException(MethodArgumentNotValidException ex) {
        ErrorResponse responseError = new ErrorResponse();
        responseError.setStatus(false);


        List<String> details = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->{
            StringBuilder errorMessage = new StringBuilder();
            details.add(errorMessage.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ").toString());
        });


        responseError.setMessage("Validation error!");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.error("Validation error!", details));
    }

}