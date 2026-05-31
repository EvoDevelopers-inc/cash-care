package evo.developers.com.cashcare.model;

public enum SpendingStyle {
    PLANNED("планирую заранее, держу бюджет"),
    MIXED("в основном планирую, но срываюсь"),
    IMPULSIVE("трачу импульсивно, плохо контролирую");

    private final String label;

    SpendingStyle(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
