package evo.developers.com.cashcare.model;

public enum SpontaneousTransactionType {
    INCOME("Доход"),
    EXPENSE("Расход");

    private final String label;

    SpontaneousTransactionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
