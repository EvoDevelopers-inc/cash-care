package evo.developers.com.cashcare.dto.base;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Response {
    private boolean status = false;
    private String message = "No payload";
}
