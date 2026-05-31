package evo.developers.com.cashcare.dto.request;

import evo.developers.com.cashcare.model.GoalCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class GoalRequest {

    @NotBlank
    @Size(max = 80)
    private String title;

    @NotNull
    private GoalCategory category;

    @Size(max = 8)
    private String customEmoji;

    @NotNull
    @DecimalMin(value = "1.0", message = "Целевая сумма должна быть больше нуля")
    private BigDecimal targetAmount;

    @DecimalMin(value = "0.0")
    private BigDecimal savedAmount;

    private Instant targetDate;
}
