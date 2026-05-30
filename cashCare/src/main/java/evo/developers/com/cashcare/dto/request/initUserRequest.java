package evo.developers.com.cashcare.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class InitUserRequest {

    @NotNull
    @PositiveOrZero
    private BigDecimal salary;

    private String others;

    @NotEmpty
    @Valid
    private List<CategoryBudgetRequest> categories;
}
