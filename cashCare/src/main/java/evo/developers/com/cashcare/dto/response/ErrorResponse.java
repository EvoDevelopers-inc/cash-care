package evo.developers.com.cashcare.dto.response;

import evo.developers.com.cashcare.dto.base.Response;
import lombok.Getter;

public class ErrorResponse extends Response {
    public static ErrorResponse error(String message, Object details) {
        ErrorResponse instance = new ErrorResponse();
        instance.setMessage(message);
        instance.setStatus(false);
        instance.details = details;

        return instance;
    }

    @Getter
    private Object details;
}
