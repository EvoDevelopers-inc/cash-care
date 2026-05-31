package evo.developers.com.cashcare.model;

public enum HousingStatus {
    OWN("своё жильё, без ипотеки"),
    MORTGAGE("своё жильё, в ипотеке"),
    RENT("снимаю жильё"),
    WITH_PARENTS("живу с родителями");

    private final String label;

    HousingStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
