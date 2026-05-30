package evo.developers.com.cashcare.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DefaultCategoryTemplate {
    HOUSING("Жильё", true),
    FOOD("Еда", true),
    TRANSPORT("Транспорт", false),
    SUBSCRIPTIONS("Подписки", false),
    ENTERTAINMENT("Развлечения", false),
    CREDITS("Кредиты", true);

    private final String name;
    private final boolean required;
}
