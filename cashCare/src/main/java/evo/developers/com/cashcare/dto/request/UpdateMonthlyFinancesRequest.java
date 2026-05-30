package evo.developers.com.cashcare.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateMonthlyFinancesRequest {

    @Min(2000)
    @Max(2100)
    private Integer year;

    @Min(1)
    @Max(12)
    private Integer month;

    @PositiveOrZero
    private BigDecimal salary;

    private String others;
}
