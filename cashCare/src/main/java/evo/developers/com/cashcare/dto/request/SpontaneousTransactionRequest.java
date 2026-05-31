package evo.developers.com.cashcare.dto.request;

import evo.developers.com.cashcare.model.SpontaneousTransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SpontaneousTransactionRequest {

    @NotNull
    private Long monthlyFinancesId;

    @NotNull
    private SpontaneousTransactionType type;

    @NotNull
    @DecimalMin(value = "0.01", message = "Сумма должна быть больше нуля")
    private BigDecimal amount;

    @Size(max = 200)
    private String note;
}
