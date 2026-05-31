package evo.developers.com.cashcare.model;

public enum CitySize {
    CAPITAL("Москва/Санкт-Петербург"),
    MILLION("город-миллионник"),
    LARGE("крупный город (300k+)"),
    SMALL("малый город / посёлок");

    private final String label;

    CitySize(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
