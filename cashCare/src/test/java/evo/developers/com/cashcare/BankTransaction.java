package evo.developers.com.cashcare;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BankTransaction {
    @JsonProperty("Дата и время операции")
    private String dateTimeOperation;
    
    @JsonProperty("Дата списания")
    private String dateTimeWriteOff;
    
    @JsonProperty("Сумма в валюте операции")
    private String amountInOpCurrency;
    
    @JsonProperty("Сумма операции в валюте карты")
    private String amountInCardCurrency;
    
    @JsonProperty("Описание операции")
    private String description;
    
    @JsonProperty("Номер карты")
    private String cardNum;
}