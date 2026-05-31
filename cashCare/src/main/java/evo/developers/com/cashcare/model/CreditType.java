package evo.developers.com.cashcare.model;

public enum CreditType {
    CONSUMER("потребкредит"),
    MORTGAGE("ипотека"),
    AUTO("автокредит"),
    CARD("кредитная карта"),
    INSTALLMENT("рассрочка"),
    MICROLOAN("микрозайм"),
    OTHER("другое");

    private final String label;

    CreditType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
