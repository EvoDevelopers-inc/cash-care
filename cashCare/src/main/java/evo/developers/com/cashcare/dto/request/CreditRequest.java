package evo.developers.com.cashcare.dto.request;

import evo.developers.com.cashcare.model.CreditType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreditRequest {

    private Long id;

    @NotBlank
    @Size(max = 80)
    private String name;

    @NotNull
    private CreditType type;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal balance;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal monthlyPayment;

    @Min(0)
    @Max(100)
    private Double interestRate;

    @Min(0)
    @Max(600)
    private Integer monthsLeft;
}
