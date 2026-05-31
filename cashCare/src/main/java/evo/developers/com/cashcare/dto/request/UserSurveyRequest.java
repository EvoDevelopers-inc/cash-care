package evo.developers.com.cashcare.dto.request;

import evo.developers.com.cashcare.model.CitySize;
import evo.developers.com.cashcare.model.EmploymentType;
import evo.developers.com.cashcare.model.FinancialGoal;
import evo.developers.com.cashcare.model.HousingStatus;
import evo.developers.com.cashcare.model.MaritalStatus;
import evo.developers.com.cashcare.model.SpendingStyle;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSurveyRequest {

    @NotNull
    private MaritalStatus maritalStatus;

    @NotNull
    @Min(0)
    @Max(20)
    private Integer childrenCount;

    @NotNull
    private EmploymentType employmentType;

    @NotNull
    private HousingStatus housingStatus;

    @NotNull
    private Boolean hasDebts;

    @NotNull
    private FinancialGoal financialGoal;

    @NotNull
    private CitySize citySize;

    @NotNull
    private SpendingStyle spendingStyle;
}
