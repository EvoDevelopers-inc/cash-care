package evo.developers.com.cashcare.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class InitSetupResponse {
    private Long monthlyFinancesId;
    private BigDecimal salary;
    private String others;
    private List<CategoryResponse> categories;
}
