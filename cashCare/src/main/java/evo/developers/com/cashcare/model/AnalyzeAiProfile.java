package evo.developers.com.cashcare.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class AnalyzeAiProfile {
    private Period period;
    private Totals totals;
    private List<Category> categories;

    @JsonProperty("detected_subscriptions")
    private List<Subscription> detectedSubscriptions;

    @JsonProperty("suggested_new_categories")
    private List<SuggestedCategory> suggestedNewCategories;

    @JsonProperty("financial_profile")
    private FinancialProfile financialProfile;

    @JsonProperty("recommended_free_pocket_pct")
    private Double recommendedFreePocketPct;

    private List<String> insights;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Period {
        @JsonProperty("start_date")
        private String startDate;

        @JsonProperty("end_date")
        private String endDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Totals {
        @JsonProperty("total_income")
        private double totalIncome;

        @JsonProperty("total_expense")
        private double totalExpense;

        private String currency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Category {
        @JsonProperty("category_name")
        @JsonAlias({"name", "category", "title"})
        private String categoryName;

        @JsonAlias({"value", "sum"})
        private double amount;

        @JsonAlias({"share", "pct"})
        private double percentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuggestedCategory {
        @JsonProperty("category_name")
        @JsonAlias({"name", "category", "title"})
        private String categoryName;

        @JsonAlias({"value", "sum"})
        private double amount;

        @JsonAlias({"share", "pct"})
        private double percentage;

        @JsonAlias({"why", "comment"})
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Subscription {
        @JsonProperty("service_name")
        @JsonAlias({"name", "service", "title"})
        private String serviceName;

        @JsonProperty("estimated_monthly_amount")
        @JsonAlias({"amount", "monthly_amount", "price"})
        private double estimatedMonthlyAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialProfile {
        @JsonProperty("personality_type")
        private String personalityType;

        private String reasoning;
    }
}
