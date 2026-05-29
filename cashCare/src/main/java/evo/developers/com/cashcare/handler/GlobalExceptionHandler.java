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

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
//        ErrorResponse responseError = new ErrorResponse();
//        return ResponseEntity
//                .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(responseError);
//    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        ErrorResponse responseError = new ErrorResponse();
        responseError.setStatus(false);
        responseError.setMessage(ex.getMessage());

        return ResponseEntity
                .status(ex.getStatusHttpCode())
                .body(responseError);
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