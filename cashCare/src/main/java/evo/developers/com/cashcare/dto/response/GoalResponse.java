package evo.developers.com.cashcare.dto.response;

import evo.developers.com.cashcare.entity.GoalContributionEntity;
import evo.developers.com.cashcare.entity.GoalEntity;
import evo.developers.com.cashcare.model.GoalCategory;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Getter
@Setter
public class GoalResponse {

    private Long id;
    private String title;
    private GoalCategory category;
    private String categoryLabel;
    private String emoji;
    private String gradient;
    private BigDecimal targetAmount;
    private BigDecimal savedAmount;
    private BigDecimal remainingAmount;
    private Integer progressPct;
    private Boolean completed;
    private Instant targetDate;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;

    private BigDecimal avgMonthlyContribution;
    private Integer etaMonths;
    private Integer monthsUntilTargetDate;
    private BigDecimal requiredMonthlyForDeadline;
    private Integer contributionsCount;
    private Boolean contributedThisMonth;
    private Instant nextContributionAt;
    private String nextContributionLabel;

    public static GoalResponse from(GoalEntity entity) {
        return from(entity, null);
    }

    public static GoalResponse from(GoalEntity entity, List<GoalContributionEntity> contributions) {
        GoalResponse r = new GoalResponse();
        r.setId(entity.getId());
        r.setTitle(entity.getTitle());

        GoalCategory cat = entity.getCategory();
        r.setCategory(cat);
        r.setCategoryLabel(cat != null ? cat.getLabel() : null);
        r.setGradient(cat != null ? cat.getGradient() : "from-slate-400 to-slate-600");

        String emoji = null;
        if (cat == GoalCategory.CUSTOM && entity.getCustomEmoji() != null && !entity.getCustomEmoji().isBlank()) {
            emoji = entity.getCustomEmoji();
        } else if (cat != null) {
            emoji = cat.getDefaultEmoji();
        }
        r.setEmoji(emoji != null ? emoji : "⭐");

        BigDecimal target = entity.getTargetAmount() != null ? entity.getTargetAmount() : BigDecimal.ZERO;
        BigDecimal saved = entity.getSavedAmount() != null ? entity.getSavedAmount() : BigDecimal.ZERO;
        BigDecimal remaining = target.subtract(saved);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;

        r.setTargetAmount(target);
        r.setSavedAmount(saved);
        r.setRemainingAmount(remaining);

        int pct = 0;
        if (target.compareTo(BigDecimal.ZERO) > 0) {
            pct = saved.multiply(BigDecimal.valueOf(100))
                    .divide(target, 0, java.math.RoundingMode.FLOOR)
                    .intValue();
            if (pct > 100) pct = 100;
            if (pct < 0) pct = 0;
        }
        r.setProgressPct(pct);

        r.setCompleted(entity.getCompletedAt() != null || pct >= 100);
        r.setTargetDate(entity.getTargetDate());
        r.setCompletedAt(entity.getCompletedAt());
        r.setCreatedAt(entity.getCreatedAt());
        r.setUpdatedAt(entity.getUpdatedAt());

        Instant now = Instant.now();
        if (entity.getTargetDate() != null) {
            long days = Duration.between(now, entity.getTargetDate()).toDays();
            int months = (int) Math.max(0, Math.round(days / 30.0));
            r.setMonthsUntilTargetDate(months);
            if (!Boolean.TRUE.equals(r.getCompleted()) && months > 0
                    && remaining.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal need = remaining.divide(
                        BigDecimal.valueOf(months), 0, RoundingMode.CEILING
                );
                r.setRequiredMonthlyForDeadline(need);
            }
        }

        ZoneId zone = ZoneId.of("Europe/Moscow");
        DateTimeFormatter humanFmt = DateTimeFormatter
                .ofPattern("d MMMM, HH:mm", new Locale("ru"));
        Duration lockPeriod = Duration.ofDays(30);
        Instant nowInstant = Instant.now();

        boolean lockedByLastContribution = false;
        Instant nextAt = null;
        if (contributions != null) {
            Instant latest = null;
            for (GoalContributionEntity c : contributions) {
                Instant t = c.getCreatedAt();
                if (t == null) continue;
                if (latest == null || t.isAfter(latest)) latest = t;
            }
            if (latest != null) {
                Instant unlock = latest.plus(lockPeriod);
                if (nowInstant.isBefore(unlock)) {
                    lockedByLastContribution = true;
                    nextAt = unlock;
                }
            }
        }

        if (Boolean.TRUE.equals(r.getCompleted())) {
            lockedByLastContribution = false;
            nextAt = null;
        }

        r.setContributedThisMonth(lockedByLastContribution);
        if (lockedByLastContribution && nextAt != null) {
            r.setNextContributionAt(nextAt);
            r.setNextContributionLabel(nextAt.atZone(zone).format(humanFmt));
        }

        if (contributions != null && !contributions.isEmpty()) {
            r.setContributionsCount(contributions.size());

            if (contributions.size() >= 2) {
                BigDecimal sumOfContributions = BigDecimal.ZERO;
                Instant earliest = null;
                Instant latest = null;
                for (GoalContributionEntity c : contributions) {
                    if (c.getAmount() != null) {
                        sumOfContributions = sumOfContributions.add(c.getAmount());
                    }
                    Instant t = c.getCreatedAt();
                    if (t != null) {
                        if (earliest == null || t.isBefore(earliest)) earliest = t;
                        if (latest == null || t.isAfter(latest)) latest = t;
                    }
                }

                if (earliest != null && latest != null && !earliest.equals(latest)) {
                    long days = Math.max(1, Duration.between(earliest, latest).toDays());
                    double monthsBetween = Math.max(1.0, days / 30.0);
                    BigDecimal avg = sumOfContributions.divide(
                            BigDecimal.valueOf(monthsBetween), 0, RoundingMode.HALF_UP
                    );
                    if (avg.compareTo(BigDecimal.ZERO) > 0) {
                        r.setAvgMonthlyContribution(avg);

                        if (!Boolean.TRUE.equals(r.getCompleted())
                                && remaining.compareTo(BigDecimal.ZERO) > 0) {
                            int eta = remaining.divide(avg, 0, RoundingMode.CEILING).intValue();
                            if (eta > 600) eta = 600;
                            r.setEtaMonths(eta);
                        }
                    }
                }
            }
        } else {
            r.setContributionsCount(0);
        }

        return r;
    }
}
