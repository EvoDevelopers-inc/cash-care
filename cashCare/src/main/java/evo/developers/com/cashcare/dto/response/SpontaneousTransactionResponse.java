package evo.developers.com.cashcare.dto.response;

import evo.developers.com.cashcare.entity.SpontaneousTransactionEntity;
import evo.developers.com.cashcare.model.SpontaneousTransactionType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Getter
@Setter
public class SpontaneousTransactionResponse {

    private Long id;
    private Long monthlyFinancesId;
    private SpontaneousTransactionType type;
    private String typeLabel;
    private BigDecimal amount;
    private String note;
    private Instant createdAt;
    private String createdAtLabel;

    public static SpontaneousTransactionResponse from(SpontaneousTransactionEntity entity) {
        SpontaneousTransactionResponse r = new SpontaneousTransactionResponse();
        r.setId(entity.getId());
        r.setMonthlyFinancesId(entity.getMonthlyFinances().getId());
        r.setType(entity.getType());
        r.setTypeLabel(entity.getType() != null ? entity.getType().getLabel() : null);
        r.setAmount(entity.getAmount());
        r.setNote(entity.getNote());
        r.setCreatedAt(entity.getCreatedAt());
        if (entity.getCreatedAt() != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM, HH:mm", new Locale("ru"));
            r.setCreatedAtLabel(entity.getCreatedAt().atZone(ZoneId.of("Europe/Moscow")).format(fmt));
        }
        return r;
    }
}
