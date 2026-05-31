# О! Зарплата

Личный финансовый ассистент с AI-анализом банковских выписок, индивидуальной анкетой, кредитным модулем и трекером мечты. Раз в месяц пользователь получает структурированный разбор плана и факта, рекомендации по «свободному кошельку» и прогресс-визуализацию по своим целям.

---

## Возможности

- **Месячный бюджет** — план дохода и категорий расходов с пометкой «обязательная / необязательная».
- **AI-анализ выписки** — загрузка PDF из T-Bank, нейронка категоризирует транзакции, находит подписки и определяет финансовый тип.
- **AI-анализ плана** — вторая модель смотрит на план + факт + анкету + кредиты + цели и отдаёт `recommended_free_pocket_pct`, три insights и оценку «хватает / впритык / не хватает».
- **Анкета пользователя** — 8 вопросов (семья, дети, работа, жильё, долги, цель, размер города, стиль трат), кормит обе AI.
- **Кредитный модуль** — кредиты и рассрочки, расчёт DTI, AI учитывает нагрузку при рекомендациях.
- **Трекер мечты** — цели с категориями, fog-эффект (мечта проявляется из тумана по мере накопления), кнопка «Я отложил» с лимитом раз в 30 дней, прогноз ETA по фактическому темпу взносов.
- **Месячный лок** — после AI-анализа бюджет и зарплата заморожены на 30 дней, чтобы план не «плыл».

---

## Стек технологий

### Бэкенд

Java 26 на Spring Boot 4.0.6. Под ним Spring Web MVC для REST-эндпоинтов, Spring Data JPA с Hibernate 6 как ORM поверх PostgreSQL и Spring Data Redis для кэша. Аутентификация — Spring Security плюс JWT через библиотеку `jjwt` 0.12.6: access-токен 1 час, refresh 7 дней. Валидация входящих DTO через Jakarta Bean Validation, документация API авто-генерируется Springdoc OpenAPI 2.8.8 и доступна по `/swagger-ui.html`. Сборка — Gradle с wrapper.

### Парсинг банковских выписок

Apache PDFBox 3.0.1 для текста и Tabula 1.0.5 для табличных данных транзакций. Выписки T-Bank структурированные, и регулярки на них развалились бы на любом изменении формата.

### Фронтенд

Сознательно без SPA-фреймворков. Чистый JavaScript плюс Tailwind CSS через Play CDN — никаких `node_modules` и сборщика. Иконки — Lucide через CDN, шрифт — Plus Jakarta Sans от Google Fonts. HTML отдаёт Thymeleaf как тонкая оболочка, вся динамика — `fetch` к REST-API.

### Хранилище

PostgreSQL 18 — основная база: пользователи, анкета, бюджеты с категориями, кредиты, цели и история взносов, кэшированный AI-анализ как fallback. Redis используется узко — два ключа на пользователя:

- `cashcare:ai:profile:{username}` — сырой JSON последнего AI-разбора выписки, TTL 30 дней.
- `cashcare:ai:cooldown:{username}` — маркер активного месячного AI-кулдауна, TTL 30 дней.

Postgres даёт целостность и долгое хранение, Redis — мгновенный ответ дашборда и автоматическое истечение замка без cron-job.

### AI-слой

OpenRouter API как единый шлюз и **Google Gemini 2.0 Flash** как основная модель. OpenRouter — чтобы не зависеть от одного провайдера и переключать модель одной строкой в конфиге. Gemini 2.0 Flash — миллионный контекст для длинных выписок, дешёвый, быстрый, стабильно держит JSON-mode.

### Инфраструктура

Docker Compose поднимает Postgres и Redis одной командой. Gradle Wrapper не требует глобальной установки Gradle.

---

## Запуск локально

Требования: Java 26 (или JDK через toolchain Gradle), Docker.

```bash
docker compose -f cashCare/docker-compose.yml up -d

cd cashCare
./gradlew bootRun
```

Приложение слушает `http://localhost:8080`. Swagger UI: `http://localhost:8080/swagger-ui.html`.

### Конфигурация

`cashCare/src/main/resources/application.properties`:

```
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=1234

jwt.secret=...
jwt.access-token-expiration-ms=3600000
jwt.refresh-token-expiration-ms=604800000

openrouter.api-key=sk-or-v1-...
openrouter.api-model=google/gemini-2.0-flash-001

data.redis.host=localhost
data.redis.port=6379
```

---

## Архитектура

Монолитное Spring Boot веб-приложение. Server-side rendering (Thymeleaf) для статической оболочки страниц + REST-API для динамики. Архитектура слоистая:

```
Browser  ←HTML+JS+CSS─  Thymeleaf Templates
         ←REST/JSON──   Spring REST Controllers (JWT)
                              │
                ┌─────────────┼──────────────┐
                ▼             ▼              ▼
           PostgreSQL 18    Redis        OpenRouter API
           (основное        (кэш AI,     (Gemini 2.0)
            хранилище)       кулдауны)
```

Слои:

- **Controller** — REST-эндпоинты, обработка `@AuthenticationPrincipal`, валидация через `@Valid`.
- **Service** — бизнес-логика, транзакции через `@Transactional`, кэш в Redis.
- **Repository** — Spring Data JPA-интерфейсы поверх Hibernate.
- **Entity** — JPA-сущности, маппинг на Postgres.
- **DTO (request/response)** — разделённые контракты для входящих и исходящих данных.

---

## Структура проекта

```
cashCare/
├── build.gradle
├── docker-compose.yml
└── src/main/
    ├── java/.../cashcare/
    │   ├── controller/
    │   ├── service/
    │   ├── jpa/
    │   ├── entity/
    │   ├── dto/
    │   │   ├── request/
    │   │   └── response/
    │   ├── model/
    │   ├── helper/
    │   ├── component/
    │   ├── config/
    │   ├── security/
    │   ├── handler/
    │   └── exception/
    └── resources/
        ├── application.properties
        ├── templates/
        └── static/
            ├── css/
            ├── js/
            └── images/
```

---

## REST API

Все эндпоинты защищены JWT (кроме `/api/auth/*`). Заголовок `Authorization: Bearer <accessToken>`.

| Префикс | Что делает |
|---|---|
| `/api/auth` | Регистрация, login, refresh, `me` |
| `/api/user` | Анкета (опросник) |
| `/api/finances/monthly` | Месячный бюджет (зарплата + init setup) |
| `/api/finances/categories` | CRUD категорий бюджета |
| `/api/finances/expenses` | Учёт расходов |
| `/api/analytics` | Дашборд-агрегат + запуск AI-анализа бюджета |
| `/api/statements` | Загрузка PDF-выписки + AI-разбор |
| `/api/credits` | CRUD кредитов и рассрочек, bulk-replace |
| `/api/goals` | CRUD целей + `POST /{id}/contribute` для «Я отложил» |

Полная схема доступна в Swagger UI после запуска.

---

## AI-pipeline

Два независимых промпта в `helper/TemplatePromptsAnalyzeAi.java`:

1. **`SYSTEM_PROMPT_ANALYZE_PDF`** — анализ банковской выписки. Категоризирует транзакции, выделяет подписки, определяет `personality_type` (Хранитель / Целеустремлённый / Импульсивный / Растерянный).
2. **`SYSTEM_PROMPT_ANALYZE_BUDGET`** — анализ месячного плана. На вход получает `salary`, `categories`, `leftover_total`, `user_profile` (анкета), `actual_spending` (из PDF, если был), `credits` и `goals`. Возвращает `recommended_free_pocket_pct`, три practical insights, оценку «хватает / впритык / не хватает».

После анализа в Redis записываются `cashcare:ai:profile:{user}` (сырой JSON) и `cashcare:ai:cooldown:{user}` (маркер). Параллельно `AnalyticsService` применяет **жёсткий floor** на `freePocket` в зависимости от соотношения leftover/salary: 3% / 5% / 8% от зарплаты — чтобы AI не выдал несоразмерно мало («Свободно 6 000 ₽» при доходе 300 000 ₽).

---

## Безопасность

- Spring Security `SecurityFilterChain` — все `/api/**` требуют JWT, кроме `/api/auth/register|login|refresh`.
- BCrypt для хэширования паролей.
- JWT access (1 ч) + refresh (7 дн), подписаны HMAC-SHA256, секрет в `application.properties`.
- Глобальный обработчик исключений `GlobalExceptionHandler` возвращает структурированный JSON с `message` и `details`, не светит стектрейсы.
- Валидация на входе через Jakarta Bean Validation (`@NotBlank`, `@DecimalMin`, `@Size` и т. д.).
- Транзакционные методы помечены `@Transactional(rollbackFor = Exception.class)` там, где важно откатывать на чекед-исключениях (регистрация).

---

## Доменные нюансы

### Месячный лок

После запуска AI-анализа бюджет и зарплата замораживаются на 30 дней. `BudgetLockService` — единый источник правды. Используется в:

- `MonthlyFinancesService` — блокировка `updateMonthlyFinances`.
- `CategoryService` — блокировка create/update/delete.
- `AnalyticsService` — блокировка повторного AI-запуска.

Замок берётся из Redis (если доступен) или из `ProfileAnalyzedAIEntity.updatedAt + 30 дней`.

### Лимит взноса в цель

`GoalService.contribute()` — максимум один взнос на конкретную цель за 30 дней (синхронно с AI-кулдауном). Реализовано через `GoalContributionRepository.findTopByGoalAndCreatedAtBetweenOrderByCreatedAtDesc(goal, now-30d, now)`.

### Денежные суммы

Везде `BigDecimal` (`NUMERIC(19,2)` в Postgres). Никаких `double` в финансовой логике.

---

## Альтернативы и обоснования

- **Java + Spring Boot vs Node.js / FastAPI** — нативный `BigDecimal`, зрелый ORM, строгая типизация ловит ошибки на этапе компиляции.
- **PostgreSQL vs MySQL** — лучше с `NUMERIC`, нормальный `JSONB`, строгие CHECK-constraints.
- **Vanilla JS vs React/Vue** — десяток виджетов на дашборде не оправдывают bundle 50 КБ + build-toolchain.
- **OpenRouter vs OpenAI напрямую** — нет vendor lock-in, переключение модели одной строкой в конфиге.
- **PDFBox + Tabula vs ручной regex-парсер** — устойчивость к изменению формата выписки.
- **JWT в `localStorage` vs HttpOnly cookie** — стандартный REST-подход, без CSRF-обвязки.

---

## Roadmap

- Миграция на Flyway/Liquibase вместо `ddl-auto=update` для прода.
- Юнит-тесты на `AnalyticsService.runBudgetAnalysis`, `GoalService.contribute`, `BudgetLockService`.
- Метрики через Spring Actuator + Prometheus.
- Поддержка выписок других банков (Сбер, Альфа).
- Push-уведомления о приближении срока цели.
