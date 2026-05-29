package evo.developers.com.cashcare.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public class ValidInputException extends BaseException{
    public ValidInputException(String message, List<String> details) {
        super(HttpStatus.CONFLICT.value(), message, details);
    }
}
