package evo.developers.com.cashcare.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BankTransaction {
    private String dateTimeOperation;

    private String dateTimeWriteOff;

    private String amountInOpCurrency;

    private String amountInCardCurrency;

    private String description;

    private String cardNum;
}
