package evo.developers.com.cashcare.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class GoalContributionRequest {

    @NotNull
    @DecimalMin(value = "1.0", message = "Сумма должна быть больше нуля")
    private BigDecimal amount;

    @Size(max = 200)
    private String note;
}
