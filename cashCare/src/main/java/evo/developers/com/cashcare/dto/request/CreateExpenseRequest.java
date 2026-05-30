package evo.developers.com.cashcare.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateExpenseRequest {

    @NotNull
    private Long monthlyFinancesId;

    @NotNull
    private Long categoryId;

    @NotNull
    @Positive
    private BigDecimal value;

    @Size(max = 500)
    private String description;
}
