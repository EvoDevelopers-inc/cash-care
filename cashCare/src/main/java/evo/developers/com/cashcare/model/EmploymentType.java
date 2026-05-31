package evo.developers.com.cashcare.model;

public enum EmploymentType {
    EMPLOYED("наёмный сотрудник"),
    SELF_EMPLOYED("самозанятый/фрилансер"),
    BUSINESS("свой бизнес"),
    STUDENT("студент"),
    UNEMPLOYED("без работы");

    private final String label;

    EmploymentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
