package evo.developers.com.cashcare.dto.response;

import evo.developers.com.cashcare.entity.ExpenseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ExpenseResponse {
    private Long id;
    private Long monthlyFinancesId;
    private Long categoryId;
    private BigDecimal value;
    private String description;

    public static ExpenseResponse from(ExpenseEntity entity) {
        ExpenseResponse response = new ExpenseResponse();
        response.setId(entity.getId());
        response.setMonthlyFinancesId(entity.getMonthlyFinances().getId());
        response.setCategoryId(entity.getCategory().getId());
        response.setValue(entity.getValue());
        response.setDescription(entity.getDescription());
        return response;
    }
}
