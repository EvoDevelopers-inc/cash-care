package evo.developers.com.cashcare.dto.response;

import evo.developers.com.cashcare.entity.MonthlyFinances;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MonthlyFinancesResponse {
    private Long id;
    private int year;
    private int month;
    private BigDecimal salary;
    private String others;

    public static MonthlyFinancesResponse from(MonthlyFinances entity) {
        MonthlyFinancesResponse response = new MonthlyFinancesResponse();
        response.setId(entity.getId());
        response.setYear(entity.getYear());
        response.setMonth(entity.getMonth());
        response.setSalary(entity.getSalary());
        response.setOthers(entity.getOthers());
        return response;
    }
}
