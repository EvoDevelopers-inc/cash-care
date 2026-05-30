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
import evo.developers.com.cashcare.entity.MonthlyFinances;
import evo.developers.com.cashcare.entity.ProfileAnalyzedAIEntity;
import evo.developers.com.cashcare.entity.UserEntity;
import evo.developers.com.cashcare.exception.NotFoundException;
import evo.developers.com.cashcare.exception.ValidInputException;
import evo.developers.com.cashcare.jpa.CategoryRepository;
import evo.developers.com.cashcare.jpa.MonthlyFinancesRepository;
import evo.developers.com.cashcare.jpa.ProfileAnalyzedAIRepository;
import evo.developers.com.cashcare.jpa.UserRepository;
import evo.developers.com.cashcare.model.AnalyzeAiProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final ZoneId ZONE = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter HUMAN_DATE = DateTimeFormatter.ofPattern("d MMMM");

    private final UserRepository userRepository;
    private final MonthlyFinancesRepository monthlyFinancesRepository;
    private final CategoryRepository categoryRepository;
    private final ProfileAnalyzedAIRepository profileAnalyzedAIRepository;
    private final JsonHelper jsonHelper;
    private final AiAnalyzeService aiAnalyzeService;

    @Transactional
    public AnalyzeAiProfile runBudgetAnalysis(String username) throws NotFoundException, ValidInputException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        ProfileAnalyzedAIEntity existing = profileAnalyzedAIRepository.findByUser(user).orElse(null);
        if (existing != null && existing.getUpdatedAt() != null
                && existing.getRecommendedFreePocketPct() != null) {
            YearMonth lastRun = YearMonth.from(existing.getUpdatedAt().atZone(ZONE).toLocalDate());
            YearMonth currentMonth = YearMonth.now(ZONE);
            if (!lastRun.isBefore(currentMonth)) {
                LocalDate nextDate = currentMonth.plusMonths(1).atDay(1);
                throw new ValidInputException(
                        "AI-анализ уже был в этом месяце. Следующий — " + nextDate.format(HUMAN_DATE.withLocale(new java.util.Locale("ru"))),
                        List.of("ai already analyzed this month")
                );
            }
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

        String json = jsonHelper.toJson(payload);
        return aiAnalyzeService.analyzeBudget(username, json);
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
        YearMonth currentMonth = YearMonth.now(ZONE);
        LocalDate nextDate = currentMonth.plusMonths(1).atDay(1);
        DateTimeFormatter humanRu = HUMAN_DATE.withLocale(new java.util.Locale("ru"));

        if (entity == null || entity.getUpdatedAt() == null
                || entity.getRecommendedFreePocketPct() == null) {
            block.setCanRefresh(true);
            block.setLastRunAt(null);
            block.setNextAvailableAt(null);
            block.setReason(null);
            return block;
        }

        Instant updatedAt = entity.getUpdatedAt();
        block.setLastRunAt(updatedAt.toString());

        YearMonth lastRun = YearMonth.from(updatedAt.atZone(ZONE).toLocalDate());
        if (lastRun.isBefore(currentMonth)) {
            block.setCanRefresh(true);
            block.setNextAvailableAt(null);
            block.setReason(null);
        } else {
            block.setCanRefresh(false);
            block.setNextAvailableAt(nextDate.toString());
            block.setReason("Уже обновляли в этом месяце. Следующий — " + nextDate.format(humanRu));
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
        ProfileAnalyzedAIEntity entity = profileAnalyzedAIRepository.findByUser(user).orElse(null);
        if (entity == null || entity.getRawJson() == null || entity.getRawJson().isBlank()) {
            return null;
        }
        try {
            return jsonHelper.fromJson(entity.getRawJson(), AnalyzeAiProfile.class);
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
