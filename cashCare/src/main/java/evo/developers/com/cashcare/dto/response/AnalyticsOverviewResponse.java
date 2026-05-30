package evo.developers.com.cashcare.dto.response;

import evo.developers.com.cashcare.dto.base.Response;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class AnalyticsOverviewResponse extends Response {

    private ProfileBlock profile;
    private MonthBlock currentMonth;
    private BalanceBlock balance;
    private List<PlannedCategory> plannedCategories;
    private AiAnalysisView aiAnalysis;
    private AiRefreshBlock aiRefresh;
    private RatingBlock rating;

    @Getter
    @Setter
    public static class ProfileBlock {
        private String username;
        private String firstName;
        private String lastName;
        private String email;
        private boolean init;
    }

    @Getter
    @Setter
    public static class MonthBlock {
        private Long id;
        private int year;
        private int month;
        private BigDecimal salary;
        private String others;
    }

    @Getter
    @Setter
    public static class BalanceBlock {
        private BigDecimal salary;
        private BigDecimal plannedExpense;
        private BigDecimal requiredExpense;
        private BigDecimal optionalExpense;
        private BigDecimal canSave;
        private BigDecimal savingsAmount;
        private BigDecimal freePocket;
        private double saveRatio;
        private String saveStatusCode;
        private String saveStatusLabel;
        private String saveTip;
        private BigDecimal aiIncome;
        private BigDecimal aiExpense;
        private BigDecimal balance;
        private String moodCode;
        private String moodLabel;
    }

    @Getter
    @Setter
    public static class PlannedCategory {
        private Long id;
        private String name;
        private boolean required;
        private BigDecimal plannedAmount;
    }

    @Getter
    @Setter
    public static class RatingBlock {
        private int rank;
        private int totalUsers;
        private double topPercent;
        private double userScore;
        private String label;
        private String tip;
    }

    @Getter
    @Setter
    public static class AiRefreshBlock {
        private boolean canRefresh;
        private String lastRunAt;
        private String nextAvailableAt;
        private String reason;
    }
}
