package evo.developers.com.cashcare.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateCategoryRequest {

    private String nameCategory;
    private Boolean required;

    @PositiveOrZero
    private BigDecimal plannedAmount;
}
