package evo.developers.com.cashcare.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public class ParsingPdfException extends BaseException {

    public ParsingPdfException(String message, List<String> errors) {
        super(HttpStatus.BAD_REQUEST.value(), message, errors);
    }
}
