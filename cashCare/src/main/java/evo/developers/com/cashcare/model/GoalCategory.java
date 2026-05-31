package evo.developers.com.cashcare.model;

public enum GoalCategory {
    CAR("Машина", "🚗", "from-slate-500 to-slate-700"),
    HOUSE("Квартира / дом", "🏠", "from-amber-400 to-orange-500"),
    RENOVATION("Ремонт", "🛋️", "from-amber-500 to-rose-500"),
    TRIP("Путешествие", "✈️", "from-sky-400 to-cyan-500"),
    EDUCATION("Образование", "🎓", "from-indigo-400 to-violet-500"),
    GADGET("Гаджет / техника", "📱", "from-zinc-500 to-slate-700"),
    WEDDING("Свадьба", "💍", "from-pink-300 to-rose-400"),
    KIDS("Ребёнок", "👶", "from-pink-400 to-fuchsia-400"),
    BUSINESS("Бизнес / стартап", "💼", "from-emerald-500 to-teal-600"),
    EMERGENCY("Подушка безопасности", "🛡️", "from-emerald-400 to-green-500"),
    DEBT_PAYOFF("Закрыть долг", "💸", "from-rose-400 to-red-500"),
    GIFT("Подарок", "🎁", "from-rose-300 to-pink-400"),
    SPORT("Спорт / фитнес", "🏋️", "from-orange-400 to-red-400"),
    HOBBY("Хобби / творчество", "🎨", "from-violet-400 to-fuchsia-500"),
    PET("Питомец", "🐶", "from-amber-300 to-orange-400"),
    HEALTH("Здоровье", "🧘", "from-teal-400 to-cyan-500"),
    INVEST("Инвестиции", "📈", "from-emerald-500 to-cyan-500"),
    PARTY("Праздник", "🎉", "from-fuchsia-400 to-pink-500"),
    MOVE("Переезд", "📦", "from-amber-400 to-yellow-500"),
    CUSTOM("Своя цель", "⭐", "from-indigo-400 to-purple-500");

    private final String label;
    private final String defaultEmoji;
    private final String gradient;

    GoalCategory(String label, String defaultEmoji, String gradient) {
        this.label = label;
        this.defaultEmoji = defaultEmoji;
        this.gradient = gradient;
    }

    public String getLabel() {
        return label;
    }

    public String getDefaultEmoji() {
        return defaultEmoji;
    }

    public String getGradient() {
        return gradient;
    }
}
