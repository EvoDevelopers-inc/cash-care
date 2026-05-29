package evo.developers.com.cashcare.exception;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BaseException extends Exception {
    private final int statusHttpCode;
    private final String message;
    private final Object details;

    protected BaseException(int statusHttpCode, String message, Object details) {
        super(message);
        this.statusHttpCode = statusHttpCode;
        this.message = message;
        this.details = details;
    }
}
