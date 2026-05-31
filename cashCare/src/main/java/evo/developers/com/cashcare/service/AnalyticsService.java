package evo.developers.com.cashcare.service;

import evo.developers.com.cashcare.component.JsonHelper;
import evo.developers.com.cashcare.dto.response.AiAnalysisView;
import evo.developers.com.cashcare.dto.response.AnalyticsOverviewResponse;
import evo.developers.com.cashcare.dto.response.AnalyticsOverviewResponse.AiRefreshBlock;
import evo.developers.com.cashcare.dto.response.AnalyticsOverviewResponse.BalanceBlock;
import evo.developers.com.cashcare.dto.response.AnalyticsOverviewResponse.MonthBlock;
import evo.developers.com.cashcare.dto.response.AnalyticsOverviewResponse.PlannedCategory;
import evo.developers.com.cashcare.dto.response.AnalyticsOverviewResponse.ProfileBlock;
import evo.developers.com.cashcare.entity.CategoryEntity;
import evo.developers.com.cashcare.entity.CreditEntity;
import evo.developers.com.cashcare.entity.GoalEntity;
import evo.developers.com.cashcare.entity.MonthlyFinances;
import evo.developers.com.cashcare.entity.ProfileAnalyzedAIEntity;
import evo.developers.com.cashcare.entity.UserEntity;
import evo.developers.com.cashcare.exception.NotFoundException;
import evo.developers.com.cashcare.exception.ValidInputException;
import evo.developers.com.cashcare.jpa.CategoryRepository;
import evo.developers.com.cashcare.jpa.CreditRepository;
import evo.developers.com.cashcare.jpa.GoalRepository;
import evo.developers.com.cashcare.jpa.MonthlyFinancesRepository;
import evo.developers.com.cashcare.jpa.ProfileAnalyzedAIRepository;
import evo.developers.com.cashcare.jpa.UserRepository;
import evo.developers.com.cashcare.model.AnalyzeAiProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserRepository userRepository;
    private final MonthlyFinancesRepository monthlyFinancesRepository;
    private final CategoryRepository categoryRepository;
    private final CreditRepository creditRepository;
    private final GoalRepository goalRepository;
    private final ProfileAnalyzedAIRepository profileAnalyzedAIRepository;
    private final JsonHelper jsonHelper;
    private final AiAnalyzeService aiAnalyzeService;
    private final RedisService redisService;
    private final BudgetLockService budgetLockService;

    @Transactional
    public AnalyzeAiProfile runBudgetAnalysis(String username) throws NotFoundException, ValidInputException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        BudgetLockService.LockState state = budgetLockService.getLockState(user);
        if (state.isLocked()) {
            throw new ValidInputException(
                    "AI-анализ уже был в этом месяце. Следующий — " + state.unlockAtHuman(),
                    List.of("ai cooldown active")
            );
        }

        MonthlyFinances mf = monthlyFinancesRepository.findTopByUserOrderByYearDescMonthDesc(user)
                .orElseThrow(() -> new NotFoundException("Monthly finances not found"));

        BigDecimal salary = mf.getSalary() != null ? mf.getSalary() : BigDecimal.ZERO;
        if (salary.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidInputException(
                    "Сначала укажи доход в бюджете",
                    List.of("salary is empty")
            );
        }

        List<CategoryEntity> categories = categoryRepository.findAllByMonthlyFinances(mf);
        boolean hasPlanned = categories.stream()
                .anyMatch(c -> c.getPlannedAmount() != null && c.getPlannedAmount().compareTo(BigDecimal.ZERO) > 0);
        if (!hasPlanned) {
            throw new ValidInputException(
                    "Заполни хотя бы одну категорию с суммой",
                    List.of("no planned categories")
            );
        }

        List<Map<String, Object>> categoryPayload = new ArrayList<>();
        BigDecimal plannedSum = BigDecimal.ZERO;
        for (CategoryEntity cat : categories) {
            BigDecimal planned = cat.getPlannedAmount() != null ? cat.getPlannedAmount() : BigDecimal.ZERO;
            if (planned.compareTo(BigDecimal.ZERO) <= 0) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", cat.getNameCategory());
            row.put("amount", planned);
            row.put("required", cat.isRequired());
            categoryPayload.add(row);
            plannedSum = plannedSum.add(planned);
        }

        BigDecimal leftover = salary.subtract(plannedSum);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("salary", salary);
        payload.put("currency", "RUB");
        payload.put("categories", categoryPayload);
        payload.put("leftover_total", leftover);
        payload.put("user_profile", buildSurveyPayload(user));

        Map<String, Object> actual = buildActualSpendingPayload(user);
        if (actual != null) {
            payload.put("actual_spending", actual);
        }

        Map<String, Object> credits = buildCreditsPayload(user, salary);
        if (credits != null) {
            payload.put("credits", credits);
        }

        Map<String, Object> goals = buildGoalsPayload(user, salary, leftover);
        if (goals != null) {
            payload.put("goals", goals);
        }

        String json = jsonHelper.toJson(payload);
        return aiAnalyzeService.analyzeBudget(username, json);
    }

    private Map<String, Object> buildGoalsPayload(UserEntity user, BigDecimal salary, BigDecimal leftover) {
        List<GoalEntity> goals = goalRepository.findAllByUserOrderByCreatedAtAsc(user);
        if (goals == null || goals.isEmpty()) return null;

        BigDecimal totalTarget = BigDecimal.ZERO;
        BigDecimal totalSaved = BigDecimal.ZERO;
        BigDecimal totalRemaining = BigDecimal.ZERO;
        int active = 0;
        int completed = 0;

        List<Map<String, Object>> rows = new ArrayList<>();
        Instant now = Instant.now();
        for (GoalEntity g : goals) {
            BigDecimal target = g.getTargetAmount() != null ? g.getTargetAmount() : BigDecimal.ZERO;
            BigDecimal saved  = g.getSavedAmount()  != null ? g.getSavedAmount()  : BigDecimal.ZERO;
            BigDecimal remaining = target.subtract(saved);
            if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;
            boolean done = g.getCompletedAt() != null || saved.compareTo(target) >= 0;

            totalTarget = totalTarget.add(target);
            totalSaved  = totalSaved.add(saved);
            totalRemaining = totalRemaining.add(remaining);
            if (done) completed++; else active++;

            int progressPct = 0;
            if (target.compareTo(BigDecimal.ZERO) > 0) {
                progressPct = saved.multiply(BigDecimal.valueOf(100))
                        .divide(target, 0, java.math.RoundingMode.FLOOR).intValue();
                if (progressPct > 100) progressPct = 100;
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("title", g.getTitle());
            row.put("category", g.getCategory() != null ? g.getCategory().name() : null);
            row.put("category_label", g.getCategory() != null ? g.getCategory().getLabel() : null);
            row.put("target_amount", target);
            row.put("saved_amount", saved);
            row.put("remaining_amount", remaining);
            row.put("progress_pct", progressPct);
            row.put("completed", done);

            if (g.getTargetDate() != null) {
                long days = Duration.between(now, g.getTargetDate()).toDays();
                int monthsLeft = (int) Math.max(1, Math.round(days / 30.0));
                row.put("months_until_target_date", monthsLeft);
                if (!done && monthsLeft > 0) {
                    BigDecimal needPerMonth = remaining.divide(
                            BigDecimal.valueOf(monthsLeft), 0, java.math.RoundingMode.CEILING
                    );
                    row.put("required_monthly_to_meet_deadline", needPerMonth);
                }
            }

            rows.add(row);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", rows);
        payload.put("active_count", active);
        payload.put("completed_count", completed);
        payload.put("total_target_amount", totalTarget);
        payload.put("total_saved_amount", totalSaved);
        payload.put("total_remaining_amount", totalRemaining);

        if (leftover != null && leftover.compareTo(BigDecimal.ZERO) > 0
                && totalRemaining.compareTo(BigDecimal.ZERO) > 0) {
            double monthsAllNeeded = totalRemaining.doubleValue() / leftover.doubleValue();
            payload.put("months_to_finish_all_at_current_leftover", Math.round(monthsAllNeeded * 10.0) / 10.0);
        }
        if (salary != null && salary.compareTo(BigDecimal.ZERO) > 0
                && totalRemaining.compareTo(BigDecimal.ZERO) > 0) {
            double pct = totalRemaining.doubleValue() / salary.doubleValue() * 100.0;
            payload.put("total_remaining_to_salary_pct", Math.round(pct * 10.0) / 10.0);
        }

        return payload;
    }

    private Map<String, Object> buildCreditsPayload(UserEntity user, BigDecimal salary) {
        List<CreditEntity> credits = creditRepository.findAllByUserOrderByIdAsc(user);
        if (credits == null || credits.isEmpty()) return null;

        BigDecimal totalBalance = BigDecimal.ZERO;
        BigDecimal totalMonthly = BigDecimal.ZERO;

        List<Map<String, Object>> rows = new ArrayList<>();
        for (CreditEntity c : credits) {
            BigDecimal balance = c.getBalance() != null ? c.getBalance() : BigDecimal.ZERO;
            BigDecimal monthly = c.getMonthlyPayment() != null ? c.getMonthlyPayment() : BigDecimal.ZERO;
            totalBalance = totalBalance.add(balance);
            totalMonthly = totalMonthly.add(monthly);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", c.getName());
            row.put("type", c.getType() != null ? c.getType().getLabel() : null);
            row.put("balance", balance);
            row.put("monthly_payment", monthly);
            row.put("interest_rate", c.getInterestRate());
            row.put("months_left", c.getMonthsLeft());
            rows.add(row);
        }

        Map<String, Object> credits_payload = new LinkedHashMap<>();
        credits_payload.put("items", rows);
        credits_payload.put("total_balance", totalBalance);
        credits_payload.put("total_monthly_payment", totalMonthly);
        if (salary != null && salary.compareTo(BigDecimal.ZERO) > 0) {
            double dti = totalMonthly.doubleValue() / salary.doubleValue() * 100.0;
            credits_payload.put("debt_to_income_pct", Math.round(dti * 10.0) / 10.0);
        }
        return credits_payload;
    }

    private Map<String, Object> buildActualSpendingPayload(UserEntity user) {
        AnalyzeAiProfile last = loadAiProfile(user);
        if (last == null) return null;

        Map<String, Object> actual = new LinkedHashMap<>();
        if (last.getPeriod() != null) {
            Map<String, Object> period = new LinkedHashMap<>();
            period.put("start_date", last.getPeriod().getStartDate());
            period.put("end_date", last.getPeriod().getEndDate());
            actual.put("period", period);
        }
        if (last.getTotals() != null) {
            Map<String, Object> totals = new LinkedHashMap<>();
            totals.put("total_income", last.getTotals().getTotalIncome());
            totals.put("total_expense", last.getTotals().getTotalExpense());
            totals.put("currency", last.getTotals().getCurrency());
            actual.put("totals", totals);
        }
        if (last.getCategories() != null && !last.getCategories().isEmpty()) {
            List<Map<String, Object>> cats = new ArrayList<>();
            for (AnalyzeAiProfile.Category c : last.getCategories()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", c.getCategoryName());
                row.put("amount", c.getAmount());
                row.put("percentage", c.getPercentage());
                cats.add(row);
            }
            actual.put("categories", cats);
        }
        if (last.getDetectedSubscriptions() != null && !last.getDetectedSubscriptions().isEmpty()) {
            List<Map<String, Object>> subs = new ArrayList<>();
            for (AnalyzeAiProfile.Subscription s : last.getDetectedSubscriptions()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("service", s.getServiceName());
                row.put("amount", s.getEstimatedMonthlyAmount());
                subs.add(row);
            }
            actual.put("subscriptions", subs);
        }
        if (last.getFinancialProfile() != null) {
            Map<String, Object> fp = new LinkedHashMap<>();
            fp.put("personality_type", last.getFinancialProfile().getPersonalityType());
            fp.put("reasoning", last.getFinancialProfile().getReasoning());
            actual.put("personality", fp);
        }
        return actual.isEmpty() ? null : actual;
    }

    private Map<String, Object> buildSurveyPayload(UserEntity user) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("age", user.getAge());
        profile.put("gender", user.getGender() != null ? user.getGender().name() : null);
        if (!user.isSurveyCompleted()) {
            profile.put("survey_completed", false);
            return profile;
        }
        profile.put("survey_completed", true);
        profile.put("marital_status", user.getMaritalStatus() != null ? user.getMaritalStatus().getLabel() : null);
        profile.put("children_count", user.getChildrenCount());
        profile.put("employment_type", user.getEmploymentType() != null ? user.getEmploymentType().getLabel() : null);
        profile.put("housing_status", user.getHousingStatus() != null ? user.getHousingStatus().getLabel() : null);
        profile.put("has_debts", user.getHasDebts());
        profile.put("financial_goal", user.getFinancialGoal() != null ? user.getFinancialGoal().getLabel() : null);
        profile.put("city_size", user.getCitySize() != null ? user.getCitySize().getLabel() : null);
        profile.put("spending_style", user.getSpendingStyle() != null ? user.getSpendingStyle().getLabel() : null);
        return profile;
    }

    @Transactional(readOnly = true)
    public AnalyticsOverviewResponse buildOverview(String username) throws NotFoundException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        AnalyticsOverviewResponse response = new AnalyticsOverviewResponse();
        response.setStatus(true);
        response.setMessage("ok");
        response.setProfile(buildProfile(user));

        MonthlyFinances current = monthlyFinancesRepository.findTopByUserOrderByYearDescMonthDesc(user).orElse(null);
        if (current != null) {
            response.setCurrentMonth(buildMonth(current));
            response.setPlannedCategories(buildPlannedCategories(current));
        } else {
            response.setPlannedCategories(List.of());
        }

        AnalyzeAiProfile aiProfile = loadAiProfile(user);
        Double aiFreePocketPct = profileAnalyzedAIRepository.findByUser(user)
                .map(ProfileAnalyzedAIEntity::getRecommendedFreePocketPct)
                .orElse(null);
        response.setAiAnalysis(aiProfile != null ? AiAnalysisView.from(aiProfile) : null);
        response.setBalance(buildBalance(current, aiProfile, aiFreePocketPct));
        response.setAiRefresh(buildAiRefresh(user));
        response.setRating(buildRating(user));

        return response;
    }

    private AnalyticsOverviewResponse.RatingBlock buildRating(UserEntity currentUser) {
        List<UserEntity> all = userRepository.findAll();
        if (all.isEmpty()) return null;

        record Score(int userId, double ratio) {}
        List<Score> scores = new ArrayList<>();
        Double currentRatio = null;

        for (UserEntity u : all) {
            MonthlyFinances mf = monthlyFinancesRepository
                    .findTopByUserOrderByYearDescMonthDesc(u).orElse(null);
            if (mf == null || mf.getSalary() == null
                    || mf.getSalary().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal salary = mf.getSalary();
            BigDecimal sumExpense = BigDecimal.ZERO;
            for (CategoryEntity c : mf.getCategories()) {
                if (c.getPlannedAmount() != null) {
                    sumExpense = sumExpense.add(c.getPlannedAmount());
                }
            }
            double ratio = salary.subtract(sumExpense).doubleValue() / salary.doubleValue();
            scores.add(new Score(u.getId(), ratio));
            if (u.getId() == currentUser.getId()) {
                currentRatio = ratio;
            }
        }

        if (currentRatio == null || scores.isEmpty()) return null;

        scores.sort((a, b) -> Double.compare(b.ratio(), a.ratio()));
        int rank = 1;
        for (int i = 0; i < scores.size(); i++) {
            if (scores.get(i).userId() == currentUser.getId()) {
                rank = i + 1;
                break;
            }
        }
        int total = scores.size();
        double topPercent = ((double) rank / total) * 100.0;

        AnalyticsOverviewResponse.RatingBlock block = new AnalyticsOverviewResponse.RatingBlock();
        block.setRank(rank);
        block.setTotalUsers(total);
        block.setTopPercent(topPercent);
        block.setUserScore(currentRatio);

        String label;
        String tip;
        if (total <= 1) {
            label = "Один в рейтинге";
            tip = "Пока ты единственный с заполненным бюджетом. Жди компанию.";
        } else if (rank == 1) {
            label = "🥇 Лидер";
            tip = "Откладываешь больше всех в системе. Держи планку.";
        } else if (topPercent <= 10) {
            label = "Топ 10%";
            tip = "В десятке самых ответственных. Подумай об инвестициях.";
        } else if (topPercent <= 25) {
            label = "Топ 25%";
            tip = "Откладываешь больше, чем 75% юзеров. Ещё чуть-чуть до топ-10%.";
        } else if (topPercent <= 50) {
            label = "Выше среднего";
            tip = "В верхней половине. Срежь 1–2 необязательные статьи и взлетишь.";
        } else if (topPercent <= 75) {
            label = "Ниже среднего";
            tip = "Большинство откладывает больше. Пересобери план — много необязательных трат.";
        } else {
            label = "Хвост рейтинга";
            tip = "Расходы съедают почти всё. Урежь хотелки или нарасти доход.";
        }
        block.setLabel(label);
        block.setTip(tip);
        return block;
    }

    private AiRefreshBlock buildAiRefresh(UserEntity user) {
        AiRefreshBlock block = new AiRefreshBlock();
        ProfileAnalyzedAIEntity entity = profileAnalyzedAIRepository.findByUser(user).orElse(null);

        if (entity != null && entity.getUpdatedAt() != null) {
            block.setLastRunAt(entity.getUpdatedAt().toString());
        }

        BudgetLockService.LockState state = budgetLockService.getLockState(user);
        if (state.isLocked()) {
            block.setCanRefresh(false);
            block.setNextAvailableAt(state.getUnlockAt().toString());
            block.setReason("Бюджет заморожен до " + state.unlockAtHuman());
        } else {
            block.setCanRefresh(true);
            block.setNextAvailableAt(null);
            block.setReason(null);
        }

        return block;
    }

    private ProfileBlock buildProfile(UserEntity user) {
        ProfileBlock block = new ProfileBlock();
        block.setUsername(user.getUsername());
        block.setFirstName(user.getFirstName());
        block.setLastName(user.getLastName());
        block.setEmail(user.getEmail());
        block.setInit(user.isInit());
        block.setSurveyCompleted(user.isSurveyCompleted());
        return block;
    }

    private MonthBlock buildMonth(MonthlyFinances mf) {
        MonthBlock block = new MonthBlock();
        block.setId(mf.getId());
        block.setYear(mf.getYear());
        block.setMonth(mf.getMonth());
        block.setSalary(mf.getSalary());
        block.setOthers(mf.getOthers());
        return block;
    }

    private List<PlannedCategory> buildPlannedCategories(MonthlyFinances mf) {
        List<CategoryEntity> categories = categoryRepository.findAllByMonthlyFinances(mf);
        List<PlannedCategory> result = new ArrayList<>(categories.size());
        for (CategoryEntity cat : categories) {
            PlannedCategory pc = new PlannedCategory();
            pc.setId(cat.getId());
            pc.setName(cat.getNameCategory());
            pc.setRequired(cat.isRequired());
            pc.setPlannedAmount(cat.getPlannedAmount() != null ? cat.getPlannedAmount() : BigDecimal.ZERO);
            result.add(pc);
        }
        return result;
    }

    private AnalyzeAiProfile loadAiProfile(UserEntity user) {
        String cacheKey = RedisService.aiProfileKey(user.getUsername());
        String cached = redisService.get(cacheKey);
        if (cached != null && !cached.isBlank()) {
            try {
                return jsonHelper.fromJson(cached, AnalyzeAiProfile.class);
            } catch (Exception ignored) {
            }
        }

        ProfileAnalyzedAIEntity entity = profileAnalyzedAIRepository.findByUser(user).orElse(null);
        if (entity == null || entity.getRawJson() == null || entity.getRawJson().isBlank()) {
            return null;
        }
        try {
            AnalyzeAiProfile profile = jsonHelper.fromJson(entity.getRawJson(), AnalyzeAiProfile.class);
            redisService.save(cacheKey, entity.getRawJson(), Duration.ofDays(30));
            return profile;
        } catch (Exception e) {
            return null;
        }
    }

    private BalanceBlock buildBalance(MonthlyFinances mf, AnalyzeAiProfile ai, Double aiFreePocketPct) {
        BalanceBlock block = new BalanceBlock();
        BigDecimal salary = mf != null && mf.getSalary() != null ? mf.getSalary() : BigDecimal.ZERO;
        block.setSalary(salary);

        BigDecimal plannedExpense = BigDecimal.ZERO;
        BigDecimal requiredExpense = BigDecimal.ZERO;
        BigDecimal optionalExpense = BigDecimal.ZERO;
        if (mf != null) {
            for (CategoryEntity cat : mf.getCategories()) {
                BigDecimal planned = cat.getPlannedAmount();
                if (planned == null) continue;
                plannedExpense = plannedExpense.add(planned);
                if (cat.isRequired()) {
                    requiredExpense = requiredExpense.add(planned);
                } else {
                    optionalExpense = optionalExpense.add(planned);
                }
            }
        }
        block.setPlannedExpense(plannedExpense);
        block.setRequiredExpense(requiredExpense);
        block.setOptionalExpense(optionalExpense);

        BigDecimal canSave = salary.subtract(plannedExpense);
        block.setCanSave(canSave);

        BigDecimal savingsAmount;
        BigDecimal freePocket;
        if (canSave.compareTo(BigDecimal.ZERO) <= 0) {
            savingsAmount = BigDecimal.ZERO;
            freePocket = BigDecimal.ZERO;
        } else {
            double pct;
            if (aiFreePocketPct != null) {
                pct = Math.max(0.0, Math.min(50.0, aiFreePocketPct));
            } else {
                pct = canSave.compareTo(BigDecimal.ZERO) > 0
                        ? Math.min(50.0, salary.multiply(BigDecimal.valueOf(0.02))
                                .divide(canSave, 4, java.math.RoundingMode.HALF_UP)
                                .doubleValue() * 100.0)
                        : 0.0;
            }
            BigDecimal targetFree = canSave
                    .multiply(BigDecimal.valueOf(pct / 100.0))
                    .setScale(0, java.math.RoundingMode.HALF_UP);

            if (salary.compareTo(BigDecimal.ZERO) > 0) {
                double leftoverRatio = canSave.doubleValue() / salary.doubleValue();
                double floorPct = 0.03;
                if (leftoverRatio > 0.50) floorPct = 0.08;
                else if (leftoverRatio > 0.30) floorPct = 0.05;

                BigDecimal floor = salary.multiply(BigDecimal.valueOf(floorPct))
                        .setScale(0, java.math.RoundingMode.HALF_UP);

                if (targetFree.compareTo(floor) < 0) {
                    targetFree = floor;
                }
            }

            if (targetFree.compareTo(canSave) > 0) targetFree = canSave;
            if (targetFree.compareTo(BigDecimal.ZERO) < 0) targetFree = BigDecimal.ZERO;
            freePocket = targetFree;
            savingsAmount = canSave.subtract(freePocket);
        }
        block.setSavingsAmount(savingsAmount);
        block.setFreePocket(freePocket);

        if (ai != null && ai.getTotals() != null) {
            block.setAiIncome(BigDecimal.valueOf(ai.getTotals().getTotalIncome()));
            block.setAiExpense(BigDecimal.valueOf(ai.getTotals().getTotalExpense()));
        } else {
            block.setAiIncome(BigDecimal.ZERO);
            block.setAiExpense(BigDecimal.ZERO);
        }

        block.setBalance(canSave);

        String moodCode;
        String moodLabel;
        String saveStatusCode;
        String saveStatusLabel;
        String saveTip;
        double saveRatio = 0.0;

        if (salary.compareTo(BigDecimal.ZERO) <= 0) {
            moodCode = "neutral";
            moodLabel = "Жду данных";
            saveStatusCode = "neutral";
            saveStatusLabel = "Нет дохода";
            saveTip = "Укажи зарплату/доход — посчитаю, сколько можно отложить.";
        } else {
            saveRatio = canSave.divide(salary, 4, java.math.RoundingMode.HALF_UP).doubleValue();

            if (saveRatio >= 0.30) {
                moodCode = "calm";
                moodLabel = "Спокойно";
                saveStatusCode = "great";
                saveStatusLabel = "Отличный резерв";
                saveTip = "Откладывай 20% автоматически — целься в подушку 3–6 зарплат.";
            } else if (saveRatio >= 0.15) {
                moodCode = "calm";
                moodLabel = "Спокойно";
                saveStatusCode = "good";
                saveStatusLabel = "Хороший резерв";
                saveTip = "Поставь автоперевод 10–15% в день зарплаты — копится незаметно.";
            } else if (saveRatio >= 0.05) {
                moodCode = "warning";
                moodLabel = "Внимание";
                saveStatusCode = "ok";
                saveStatusLabel = "Едва остаётся";
                saveTip = "Срежь 1–2 необязательные категории — освободишь место под накопления.";
            } else if (saveRatio >= 0) {
                moodCode = "warning";
                moodLabel = "Внимание";
                saveStatusCode = "tight";
                saveStatusLabel = "В ноль";
                saveTip = "Свободных денег нет. Пересобери план — без этого не накопить.";
            } else {
                moodCode = "danger";
                moodLabel = "Тревога";
                saveStatusCode = "minus";
                saveStatusLabel = "Минус по месяцу";
                saveTip = "Расходы выше дохода. Уменьши необязательные категории, иначе уйдёшь в долг.";
            }
        }

        block.setMoodCode(moodCode);
        block.setMoodLabel(moodLabel);
        block.setSaveRatio(Math.max(saveRatio, 0));
        block.setSaveStatusCode(saveStatusCode);
        block.setSaveStatusLabel(saveStatusLabel);
        block.setSaveTip(saveTip);

        return block;
    }
}
