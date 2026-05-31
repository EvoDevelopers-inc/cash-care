package evo.developers.com.cashcare.model;

public enum MaritalStatus {
    SINGLE("холост/не замужем"),
    MARRIED("в браке"),
    COHABITING("живу с партнёром"),
    DIVORCED("разведён(а)");

    private final String label;

    MaritalStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
