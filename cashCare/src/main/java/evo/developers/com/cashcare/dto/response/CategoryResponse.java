package evo.developers.com.cashcare.dto.response;

import evo.developers.com.cashcare.entity.CategoryEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CategoryResponse {
    private Long id;
    private Long monthlyFinancesId;
    private String nameCategory;
    private boolean required;
    private BigDecimal plannedAmount;

    public static CategoryResponse from(CategoryEntity entity) {
        CategoryResponse response = new CategoryResponse();
        response.setId(entity.getId());
        response.setMonthlyFinancesId(entity.getMonthlyFinances().getId());
        response.setNameCategory(entity.getNameCategory());
        response.setRequired(entity.isRequired());
        response.setPlannedAmount(entity.getPlannedAmount());
        return response;
    }
}
