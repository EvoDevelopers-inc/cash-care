package evo.developers.com.cashcare.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateExpenseRequest {

    @Positive
    private BigDecimal value;

    @Size(max = 500)
    private String description;

    private Long categoryId;
}
