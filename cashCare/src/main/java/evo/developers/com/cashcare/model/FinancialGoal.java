package evo.developers.com.cashcare.model;

public enum FinancialGoal {
    SAVE_CUSHION("накопить подушку безопасности"),
    PAY_DEBT("закрыть долги/кредиты"),
    BIG_PURCHASE("крупная покупка (авто, техника, отпуск)"),
    BUY_PROPERTY("копить на жильё"),
    INVEST("инвестировать и приумножать"),
    JUST_LIVE("жить комфортно без долгов");

    private final String label;

    FinancialGoal(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
