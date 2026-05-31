package evo.developers.com.cashcare.dto.response;

import evo.developers.com.cashcare.dto.base.Response;
import evo.developers.com.cashcare.entity.UserEntity;
import evo.developers.com.cashcare.model.CitySize;
import evo.developers.com.cashcare.model.EmploymentType;
import evo.developers.com.cashcare.model.FinancialGoal;
import evo.developers.com.cashcare.model.HousingStatus;
import evo.developers.com.cashcare.model.MaritalStatus;
import evo.developers.com.cashcare.model.SpendingStyle;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSurveyResponse extends Response {

    private boolean completed;
    private MaritalStatus maritalStatus;
    private Integer childrenCount;
    private EmploymentType employmentType;
    private HousingStatus housingStatus;
    private Boolean hasDebts;
    private FinancialGoal financialGoal;
    private CitySize citySize;
    private SpendingStyle spendingStyle;

    public static UserSurveyResponse from(UserEntity user) {
        UserSurveyResponse r = new UserSurveyResponse();
        r.setStatus(true);
        r.setMessage("ok");
        r.setCompleted(user.isSurveyCompleted());
        r.setMaritalStatus(user.getMaritalStatus());
        r.setChildrenCount(user.getChildrenCount());
        r.setEmploymentType(user.getEmploymentType());
        r.setHousingStatus(user.getHousingStatus());
        r.setHasDebts(user.getHasDebts());
        r.setFinancialGoal(user.getFinancialGoal());
        r.setCitySize(user.getCitySize());
        r.setSpendingStyle(user.getSpendingStyle());
        return r;
    }
}
