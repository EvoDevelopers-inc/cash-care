package evo.developers.com.cashcare.dto.response;

import evo.developers.com.cashcare.model.AnalyzeAiProfile;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class AiAnalysisView {
    private PeriodView period;
    private TotalsView totals;
    private List<CategoryView> categories;
    private List<SubscriptionView> detectedSubscriptions;
    private List<SuggestedCategoryView> suggestedNewCategories;
    private FinancialProfileView financialProfile;
    private List<String> insights;

    public static AiAnalysisView from(AnalyzeAiProfile profile) {
        if (profile == null) {
            return null;
        }

        AiAnalysisView view = new AiAnalysisView();

        if (profile.getPeriod() != null) {
            PeriodView period = new PeriodView();
            period.setStartDate(profile.getPeriod().getStartDate());
            period.setEndDate(profile.getPeriod().getEndDate());
            view.setPeriod(period);
        }

        if (profile.getTotals() != null) {
            TotalsView totals = new TotalsView();
            totals.setTotalIncome(profile.getTotals().getTotalIncome());
            totals.setTotalExpense(profile.getTotals().getTotalExpense());
            totals.setCurrency(profile.getTotals().getCurrency());
            view.setTotals(totals);
        }

        view.setCategories(profile.getCategories() == null
                ? Collections.emptyList()
                : profile.getCategories().stream().map(CategoryView::from).toList());

        view.setDetectedSubscriptions(profile.getDetectedSubscriptions() == null
                ? Collections.emptyList()
                : profile.getDetectedSubscriptions().stream().map(SubscriptionView::from).toList());

        view.setSuggestedNewCategories(profile.getSuggestedNewCategories() == null
                ? Collections.emptyList()
                : profile.getSuggestedNewCategories().stream().map(SuggestedCategoryView::from).toList());

        if (profile.getFinancialProfile() != null) {
            FinancialProfileView financialProfile = new FinancialProfileView();
            financialProfile.setPersonalityType(profile.getFinancialProfile().getPersonalityType());
            financialProfile.setReasoning(profile.getFinancialProfile().getReasoning());
            view.setFinancialProfile(financialProfile);
        }

        view.setInsights(profile.getInsights() == null ? Collections.emptyList() : profile.getInsights());
        return view;
    }

    @Getter
    @Setter
    public static class PeriodView {
        private String startDate;
        private String endDate;
    }

    @Getter
    @Setter
    public static class TotalsView {
        private double totalIncome;
        private double totalExpense;
        private String currency;
    }

    @Getter
    @Setter
    public static class CategoryView {
        private String categoryName;
        private double amount;
        private double percentage;

        public static CategoryView from(AnalyzeAiProfile.Category category) {
            CategoryView view = new CategoryView();
            view.setCategoryName(category.getCategoryName());
            view.setAmount(category.getAmount());
            view.setPercentage(category.getPercentage());
            return view;
        }
    }

    @Getter
    @Setter
    public static class SuggestedCategoryView {
        private String categoryName;
        private double amount;
        private double percentage;
        private String reason;

        public static SuggestedCategoryView from(AnalyzeAiProfile.SuggestedCategory category) {
            SuggestedCategoryView view = new SuggestedCategoryView();
            view.setCategoryName(category.getCategoryName());
            view.setAmount(category.getAmount());
            view.setPercentage(category.getPercentage());
            view.setReason(category.getReason());
            return view;
        }
    }

    @Getter
    @Setter
    public static class SubscriptionView {
        private String serviceName;
        private double estimatedMonthlyAmount;

        public static SubscriptionView from(AnalyzeAiProfile.Subscription subscription) {
            SubscriptionView view = new SubscriptionView();
            view.setServiceName(subscription.getServiceName());
            view.setEstimatedMonthlyAmount(subscription.getEstimatedMonthlyAmount());
            return view;
        }
    }

    @Getter
    @Setter
    public static class FinancialProfileView {
        private String personalityType;
        private String reasoning;
    }
}
