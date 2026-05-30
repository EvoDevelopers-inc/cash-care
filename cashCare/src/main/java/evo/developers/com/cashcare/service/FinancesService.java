package evo.developers.com.cashcare.service;

import evo.developers.com.cashcare.dto.request.CategoryBudgetRequest;
import evo.developers.com.cashcare.dto.request.CreateCategoryRequest;
import evo.developers.com.cashcare.dto.request.CreateExpenseRequest;
import evo.developers.com.cashcare.dto.request.CreateMonthlyFinancesRequest;
import evo.developers.com.cashcare.dto.request.InitUserRequest;
import evo.developers.com.cashcare.dto.request.UpdateCategoryRequest;
import evo.developers.com.cashcare.dto.request.UpdateExpenseRequest;
import evo.developers.com.cashcare.dto.request.UpdateMonthlyFinancesRequest;
import evo.developers.com.cashcare.dto.response.CategoryResponse;
import evo.developers.com.cashcare.dto.response.ExpenseResponse;
import evo.developers.com.cashcare.dto.response.InitSetupResponse;
import evo.developers.com.cashcare.dto.response.MonthlyFinancesResponse;
import evo.developers.com.cashcare.entity.CategoryEntity;
import evo.developers.com.cashcare.entity.ExpenseEntity;
import evo.developers.com.cashcare.entity.MonthlyFinances;
import evo.developers.com.cashcare.entity.UserEntity;
import evo.developers.com.cashcare.exception.BaseException;
import evo.developers.com.cashcare.exception.NotFoundException;
import evo.developers.com.cashcare.exception.ValidInputException;
import evo.developers.com.cashcare.jpa.CategoryRepository;
import evo.developers.com.cashcare.jpa.ExpenseRepository;
import evo.developers.com.cashcare.jpa.MonthlyFinancesRepository;
import evo.developers.com.cashcare.jpa.UserRepository;
import evo.developers.com.cashcare.model.AnalyzeAiProfile;
import evo.developers.com.cashcare.model.DefaultCategoryTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinancesService {

    private final MonthlyFinancesRepository monthlyFinancesRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private static final Map<String, String> AI_CATEGORY_ALIASES = Map.ofEntries(
            Map.entry("продукты", "еда"),
            Map.entry("продукты питания", "еда"),
            Map.entry("супермаркет", "еда"),
            Map.entry("groceries", "еда"),
            Map.entry("food", "еда"),
            Map.entry("жилье", "жильё"),
            Map.entry("аренда", "жильё"),
            Map.entry("жкх", "жильё"),
            Map.entry("кредит", "кредиты"),
            Map.entry("кредиты", "кредиты"),
            Map.entry("займ", "кредиты"),
            Map.entry("развлечения", "развлечения"),
            Map.entry("транспорт", "транспорт"),
            Map.entry("такси", "транспорт"),
            Map.entry("подписки", "подписки"),
            Map.entry("subscriptions", "подписки")
    );

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<MonthlyFinancesResponse> getMonthlyFinancesList(String username) throws NotFoundException {
        UserEntity user = getUser(username);
        return monthlyFinancesRepository.findAllByUserOrderByYearDescMonthDesc(user).stream()
                .map(MonthlyFinancesResponse::from)
                .toList();
    }

    public MonthlyFinances getMonthlyFinances(UserEntity user) throws NotFoundException {
        return monthlyFinancesRepository.
                findTopByUserOrderByYearDescMonthDesc(user).orElseGet(null);
    }

    @Transactional(readOnly = true)
    public MonthlyFinancesResponse getMonthlyFinances(String username, Long id) throws NotFoundException {
        return MonthlyFinancesResponse.from(getOwnedMonthlyFinances(username, id));
    }

    @Transactional
    public void setupDefaultFinancesForUser(UserEntity user) {
        LocalDate now = LocalDate.now();

        MonthlyFinances monthlyFinances = new MonthlyFinances();
        monthlyFinances.setUser(user);
        monthlyFinances.setYear(now.getYear());
        monthlyFinances.setMonth(now.getMonthValue());
        monthlyFinances.setSalary(BigDecimal.ZERO);
        monthlyFinances = monthlyFinancesRepository.save(monthlyFinances);

        for (DefaultCategoryTemplate template : DefaultCategoryTemplate.values()) {
            CategoryEntity category = new CategoryEntity();
            category.setNameCategory(template.getName());
            category.setRequired(template.isRequired());
            monthlyFinances.addCategory(category);
            categoryRepository.save(category);
        }
    }

    @Transactional(readOnly = true)
    public InitSetupResponse getInitSetup(String username) throws BaseException {
        UserEntity user = getUser(username);

        if (user.isInit()) {
            throw new ValidInputException("User is already initialized", List.of("already initialized"));
        }

        MonthlyFinances monthlyFinances = getCurrentMonthlyFinances(user);
        InitSetupResponse response = new InitSetupResponse();
        response.setMonthlyFinancesId(monthlyFinances.getId());
        response.setSalary(monthlyFinances.getSalary());
        response.setOthers(monthlyFinances.getOthers());
        response.setCategories(categoryRepository.findAllByMonthlyFinances(monthlyFinances).stream()
                .map(CategoryResponse::from)
                .toList());
        return response;
    }

    @Transactional
    public MonthlyFinancesResponse initMonthlyFinances(String username, InitUserRequest request) throws BaseException {
        UserEntity user = getUser(username);

        if (user.isInit()) {
            throw new ValidInputException("User is already initialized", List.of("already initialized"));
        }

        MonthlyFinances monthlyFinances = getCurrentMonthlyFinances(user);
        monthlyFinances.setSalary(request.getSalary());
        monthlyFinances.setOthers(request.getOthers());

        applyCategoryBudgets(monthlyFinances, request.getCategories());

        user.setInit(true);
        userRepository.save(user);

        return MonthlyFinancesResponse.from(monthlyFinancesRepository.save(monthlyFinances));
    }

    @Transactional
    public MonthlyFinancesResponse createMonthlyFinances(String username, CreateMonthlyFinancesRequest request)
            throws BaseException {
        UserEntity user = getUser(username);

        if (monthlyFinancesRepository.existsByUserAndYearAndMonth(user, request.getYear(), request.getMonth())) {
            throw new ValidInputException(
                    "Monthly finances for this period already exist",
                    List.of("year/month already exists")
            );
        }

        MonthlyFinances monthlyFinances = new MonthlyFinances();
        monthlyFinances.setUser(user);
        monthlyFinances.setYear(request.getYear());
        monthlyFinances.setMonth(request.getMonth());
        monthlyFinances.setSalary(request.getSalary());
        monthlyFinances.setOthers(request.getOthers());

        return MonthlyFinancesResponse.from(monthlyFinancesRepository.save(monthlyFinances));
    }

    @Transactional
    public MonthlyFinancesResponse updateMonthlyFinances(String username, Long id, UpdateMonthlyFinancesRequest request)
            throws BaseException {
        MonthlyFinances monthlyFinances = getOwnedMonthlyFinances(username, id);

        if (request.getYear() != null) {
            monthlyFinances.setYear(request.getYear());
        }
        if (request.getMonth() != null) {
            monthlyFinances.setMonth(request.getMonth());
        }
        if (request.getSalary() != null) {
            monthlyFinances.setSalary(request.getSalary());
        }
        if (request.getOthers() != null) {
            monthlyFinances.setOthers(request.getOthers());
        }

        validateUniquePeriod(monthlyFinances);

        return MonthlyFinancesResponse.from(monthlyFinancesRepository.save(monthlyFinances));
    }

    @Transactional
    public void deleteMonthlyFinances(String username, Long id) throws NotFoundException {
        MonthlyFinances monthlyFinances = getOwnedMonthlyFinances(username, id);
        monthlyFinancesRepository.delete(monthlyFinances);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(String username, Long monthlyFinancesId) throws NotFoundException {
        MonthlyFinances monthlyFinances = getOwnedMonthlyFinances(username, monthlyFinancesId);
        return categoryRepository.findAllByMonthlyFinances(monthlyFinances).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(String username, Long id) throws NotFoundException {
        return CategoryResponse.from(getOwnedCategory(username, id));
    }


    @Transactional
    public CategoryResponse createCategory(String username, CreateCategoryRequest request) throws NotFoundException {
        return createCategory(
                username,
                request.getMonthlyFinancesId(),
                request.getNameCategory(),
                request.isRequired()
        );
    }

    @Transactional
    public CategoryResponse createCategory(String username, long monthlyFinancesId, String nameCategory, boolean required) throws NotFoundException {
        MonthlyFinances monthlyFinances = getOwnedMonthlyFinances(username, monthlyFinancesId);

        CategoryEntity category = new CategoryEntity();
        category.setNameCategory(nameCategory);
        category.setRequired(required);
        monthlyFinances.addCategory(category);

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public void applyAiAnalysis(String username, AnalyzeAiProfile profile) throws NotFoundException {
        if (profile == null) {
            return;
        }

        applyAiCategories(username, profile.getCategories(), profile.getPeriod());

        if (profile.getDetectedSubscriptions() != null && !profile.getDetectedSubscriptions().isEmpty()) {
            double subscriptionTotal = profile.getDetectedSubscriptions().stream()
                    .mapToDouble(AnalyzeAiProfile.Subscription::getEstimatedMonthlyAmount)
                    .sum();

            if (subscriptionTotal > 0) {
                upsertAiCategory(username, DefaultCategoryTemplate.SUBSCRIPTIONS.getName(), subscriptionTotal, true);
            }
        }
    }

    @Transactional
    public void applyAiCategories(
            String username,
            List<AnalyzeAiProfile.Category> aiCategories,
            AnalyzeAiProfile.Period period
    ) throws NotFoundException {
        if (aiCategories == null || aiCategories.isEmpty()) {
            return;
        }

        UserEntity user = getUser(username);
        MonthlyFinances monthlyFinances = getCurrentMonthlyFinances(user);
        Map<String, CategoryEntity> categoriesByKey = indexCategories(
                categoryRepository.findAllByMonthlyFinances(monthlyFinances)
        );

        for (AnalyzeAiProfile.Category aiCategory : aiCategories) {
            if (aiCategory == null || aiCategory.getCategoryName() == null || aiCategory.getCategoryName().isBlank()) {
                continue;
            }

            String resolvedName = resolveAiCategoryName(aiCategory.getCategoryName());
            CategoryEntity category = findExistingCategory(categoriesByKey, resolvedName);

            if (category == null) {
                category = new CategoryEntity();
                category.setNameCategory(resolvedName);
                category.setRequired(isDefaultRequiredCategory(resolvedName));
                monthlyFinances.addCategory(category);
                category = categoryRepository.save(category);
                categoriesByKey.put(normalizeCategoryKey(category.getNameCategory()), category);
            }

            if (aiCategory.getAmount() > 0) {
                category.setPlannedAmount(toMonthlyPlannedAmount(aiCategory.getAmount(), period));
                categoryRepository.save(category);
            }
        }
    }

    private void upsertAiCategory(String username, String categoryName, double amount, boolean alreadyMonthly) throws NotFoundException {
        UserEntity user = getUser(username);
        MonthlyFinances monthlyFinances = getCurrentMonthlyFinances(user);
        Map<String, CategoryEntity> categoriesByKey = indexCategories(
                categoryRepository.findAllByMonthlyFinances(monthlyFinances)
        );

        CategoryEntity category = findExistingCategory(categoriesByKey, categoryName);
        if (category == null) {
            category = new CategoryEntity();
            category.setNameCategory(categoryName);
            category.setRequired(isDefaultRequiredCategory(categoryName));
            monthlyFinances.addCategory(category);
            category = categoryRepository.save(category);
        }

        BigDecimal plannedAmount = alreadyMonthly
                ? BigDecimal.valueOf(amount).setScale(0, RoundingMode.HALF_UP)
                : toMonthlyPlannedAmount(amount, null);
        category.setPlannedAmount(plannedAmount);
        categoryRepository.save(category);
    }

    private Map<String, CategoryEntity> indexCategories(List<CategoryEntity> categories) {
        Map<String, CategoryEntity> indexed = new HashMap<>();
        for (CategoryEntity category : categories) {
            indexed.putIfAbsent(normalizeCategoryKey(category.getNameCategory()), category);
        }
        return indexed;
    }

    private CategoryEntity findExistingCategory(Map<String, CategoryEntity> categoriesByKey, String categoryName) {
        String key = normalizeCategoryKey(categoryName);
        CategoryEntity category = categoriesByKey.get(key);
        if (category != null) {
            return category;
        }

        String aliasTarget = AI_CATEGORY_ALIASES.get(key);
        if (aliasTarget != null) {
            return categoriesByKey.get(aliasTarget);
        }

        return null;
    }

    private String resolveAiCategoryName(String rawName) {
        String trimmed = rawName.trim();
        String key = normalizeCategoryKey(trimmed);
        String aliasTarget = AI_CATEGORY_ALIASES.get(key);
        if (aliasTarget == null) {
            return trimmed;
        }

        return matchDefaultCategoryName(aliasTarget);
    }

    private String matchDefaultCategoryName(String normalizedAlias) {
        for (DefaultCategoryTemplate template : DefaultCategoryTemplate.values()) {
            if (normalizeCategoryKey(template.getName()).equals(normalizedAlias)) {
                return template.getName();
            }
        }
        return normalizedAlias;
    }

    private boolean isDefaultRequiredCategory(String categoryName) {
        for (DefaultCategoryTemplate template : DefaultCategoryTemplate.values()) {
            if (template.getName().equalsIgnoreCase(categoryName)) {
                return template.isRequired();
            }
        }
        return false;
    }

    private String normalizeCategoryKey(String name) {
        return name.trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
    }

    private BigDecimal toMonthlyPlannedAmount(double statementAmount, AnalyzeAiProfile.Period period) {
        long monthsInPeriod = estimateMonthsInPeriod(period);
        double monthlyAmount = statementAmount / monthsInPeriod;
        return BigDecimal.valueOf(monthlyAmount).setScale(0, RoundingMode.HALF_UP);
    }

    private long estimateMonthsInPeriod(AnalyzeAiProfile.Period period) {
        if (period == null || period.getStartDate() == null || period.getEndDate() == null) {
            return 1L;
        }

        try {
            LocalDate start = LocalDate.parse(period.getStartDate());
            LocalDate end = LocalDate.parse(period.getEndDate());
            long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
            return Math.max(1L, Math.round(days / 30.0));
        } catch (Exception _) {
            return 1L;
        }
    }

    @Transactional
    public CategoryResponse updateCategory(String username, Long id, UpdateCategoryRequest request)
            throws NotFoundException {
        CategoryEntity category = getOwnedCategory(username, id);

        if (request.getNameCategory() != null && !request.getNameCategory().isBlank()) {
            category.setNameCategory(request.getNameCategory());
        }
        if (request.getRequired() != null) {
            category.setRequired(request.getRequired());
        }
        if (request.getPlannedAmount() != null) {
            category.setPlannedAmount(request.getPlannedAmount());
        }

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(String username, Long id) throws NotFoundException {
        CategoryEntity category = getOwnedCategory(username, id);
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpenses(String username, Long monthlyFinancesId, Long categoryId)
            throws BaseException {
        if (categoryId != null) {
            CategoryEntity category = getOwnedCategory(username, categoryId);
            return expenseRepository.findAllByCategory(category).stream()
                    .map(ExpenseResponse::from)
                    .toList();
        }

        if (monthlyFinancesId == null) {
            throw new ValidInputException(
                    "monthlyFinancesId or categoryId is required",
                    List.of("monthlyFinancesId is required")
            );
        }

        MonthlyFinances monthlyFinances = getOwnedMonthlyFinances(username, monthlyFinancesId);
        return expenseRepository.findAllByMonthlyFinances(monthlyFinances).stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpense(String username, Long id) throws NotFoundException {
        return ExpenseResponse.from(getOwnedExpense(username, id));
    }

    @Transactional
    public ExpenseResponse createExpense(String username, CreateExpenseRequest request) throws BaseException {
        MonthlyFinances monthlyFinances = getOwnedMonthlyFinances(username, request.getMonthlyFinancesId());
        CategoryEntity category = getOwnedCategory(username, request.getCategoryId());

        if (!category.getMonthlyFinances().getId().equals(monthlyFinances.getId())) {
            throw new ValidInputException(
                    "Category does not belong to selected monthly finances",
                    List.of("categoryId mismatch")
            );
        }

        ExpenseEntity expense = new ExpenseEntity();
        expense.setValue(request.getValue());
        expense.setDescription(request.getDescription());
        category.addExpense(expense);

        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse updateExpense(String username, Long id, UpdateExpenseRequest request) throws BaseException {
        ExpenseEntity expense = getOwnedExpense(username, id);

        if (request.getValue() != null) {
            expense.setValue(request.getValue());
        }
        if (request.getDescription() != null) {
            expense.setDescription(request.getDescription());
        }
        if (request.getCategoryId() != null) {
            CategoryEntity category = getOwnedCategory(username, request.getCategoryId());
            if (!category.getMonthlyFinances().getId().equals(expense.getMonthlyFinances().getId())) {
                throw new ValidInputException(
                        "Category does not belong to this monthly finances",
                        List.of("categoryId mismatch")
                );
            }
            expense.setCategory(category);
        }

        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @Transactional
    public void deleteExpense(String username, Long id) throws NotFoundException {
        ExpenseEntity expense = getOwnedExpense(username, id);
        expenseRepository.delete(expense);
    }

    private UserEntity getUser(String username) throws NotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private MonthlyFinances getOwnedMonthlyFinances(String username, Long id) throws NotFoundException {
        UserEntity user = getUser(username);
        return monthlyFinancesRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Monthly finances not found"));
    }

    private CategoryEntity getOwnedCategory(String username, Long id) throws NotFoundException {
        UserEntity user = getUser(username);
        return categoryRepository.findByIdAndMonthlyFinances_User(id, user)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private ExpenseEntity getOwnedExpense(String username, Long id) throws NotFoundException {
        UserEntity user = getUser(username);
        return expenseRepository.findByIdAndMonthlyFinances_User(id, user)
                .orElseThrow(() -> new NotFoundException("Expense not found"));
    }

    private MonthlyFinances getCurrentMonthlyFinances(UserEntity user) throws NotFoundException {
        LocalDate now = LocalDate.now();
        return monthlyFinancesRepository.findByUserAndYearAndMonth(user, now.getYear(), now.getMonthValue())
                .orElseThrow(() -> new NotFoundException("Monthly finances not found"));
    }

    private void applyCategoryBudgets(MonthlyFinances monthlyFinances, List<CategoryBudgetRequest> budgets)
            throws ValidInputException {
        List<CategoryEntity> categories = categoryRepository.findAllByMonthlyFinances(monthlyFinances);

        if (budgets.size() != categories.size()) {
            throw new ValidInputException(
                    "Provide planned amounts for all categories",
                    List.of("all categories are required")
            );
        }

        Map<Long, CategoryEntity> categoriesById = categories.stream()
                .collect(Collectors.toMap(CategoryEntity::getId, Function.identity()));

        for (CategoryBudgetRequest budget : budgets) {
            CategoryEntity category = categoriesById.get(budget.getCategoryId());
            if (category == null) {
                throw new ValidInputException("Invalid category id", List.of("categoryId: " + budget.getCategoryId()));
            }
            category.setPlannedAmount(budget.getPlannedAmount());
            categoryRepository.save(category);
        }
    }

    private void validateUniquePeriod(MonthlyFinances monthlyFinances) throws ValidInputException {
        var existing = monthlyFinancesRepository.findByUserAndYearAndMonth(
                monthlyFinances.getUser(),
                monthlyFinances.getYear(),
                monthlyFinances.getMonth()
        );

        if (existing.isPresent() && !existing.get().getId().equals(monthlyFinances.getId())) {
            throw new ValidInputException(
                    "Monthly finances for this period already exist",
                    List.of("year/month already exists")
            );
        }
    }
}
