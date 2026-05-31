package evo.developers.com.cashcare.dto.response;

import evo.developers.com.cashcare.entity.CreditEntity;
import evo.developers.com.cashcare.model.CreditType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreditResponse {

    private Long id;
    private String name;
    private CreditType type;
    private String typeLabel;
    private BigDecimal balance;
    private BigDecimal monthlyPayment;
    private Double interestRate;
    private Integer monthsLeft;

    public static CreditResponse from(CreditEntity entity) {
        CreditResponse r = new CreditResponse();
        r.setId(entity.getId());
        r.setName(entity.getName());
        r.setType(entity.getType());
        r.setTypeLabel(entity.getType() != null ? entity.getType().getLabel() : null);
        r.setBalance(entity.getBalance());
        r.setMonthlyPayment(entity.getMonthlyPayment());
        r.setInterestRate(entity.getInterestRate());
        r.setMonthsLeft(entity.getMonthsLeft());
        return r;
    }
}
