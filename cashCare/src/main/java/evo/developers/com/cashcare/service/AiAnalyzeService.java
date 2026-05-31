package evo.developers.com.cashcare.service;

import evo.developers.com.cashcare.component.JsonHelper;
import evo.developers.com.cashcare.component.OpenRouterClient;
import evo.developers.com.cashcare.entity.ProfileAnalyzedAIEntity;
import evo.developers.com.cashcare.entity.UserEntity;
import evo.developers.com.cashcare.exception.NotFoundException;
import evo.developers.com.cashcare.helper.TemplatePromptsAnalyzeAi;
import evo.developers.com.cashcare.jpa.ProfileAnalyzedAIRepository;
import evo.developers.com.cashcare.jpa.UserRepository;
import evo.developers.com.cashcare.model.AnalyzeAiProfile;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@AllArgsConstructor
public class AiAnalyzeService {

    private static final Pattern CODE_FENCE = Pattern.compile(
            "(?s)```(?:json)?\\s*(.*?)\\s*```",
            Pattern.CASE_INSENSITIVE
    );

    private static final Duration AI_PROFILE_TTL = Duration.ofDays(30);
    private static final Duration AI_COOLDOWN_TTL = Duration.ofDays(30);

    private final OpenRouterClient openRouterClient;
    private final JsonHelper jsonHelper;
    private final FinancesService financesService;
    private final UserRepository userRepository;
    private final ProfileAnalyzedAIRepository profileAnalyzedAIRepository;
    private final RedisService redisService;

    @Transactional
    public AnalyzeAiProfile analyzeTransaction(String username, String payload) throws NotFoundException {
        String response = openRouterClient.ask(
                TemplatePromptsAnalyzeAi.SYSTEM_PROMPT_ANALYZE_STATEMENT_PDF,
                payload
        );

        String json = sanitizeJsonResponse(response);
        AnalyzeAiProfile profile = jsonHelper.fromJson(json, AnalyzeAiProfile.class);

        financesService.applyAiAnalysis(username, profile);
        persistAiProfile(username, profile, json);

        return profile;
    }

    @Transactional
    public AnalyzeAiProfile analyzeBudget(String username, String payload) throws NotFoundException {
        String response = openRouterClient.ask(
                TemplatePromptsAnalyzeAi.SYSTEM_PROMPT_ANALYZE_BUDGET,
                payload
        );

        String json = sanitizeJsonResponse(response);
        AnalyzeAiProfile profile = jsonHelper.fromJson(json, AnalyzeAiProfile.class);

        persistAiProfile(username, profile, json);

        return profile;
    }

    private void persistAiProfile(String username, AnalyzeAiProfile profile, String rawJson) throws NotFoundException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        ProfileAnalyzedAIEntity entity = profileAnalyzedAIRepository.findByUser(user)
                .orElseGet(ProfileAnalyzedAIEntity::new);

        entity.setUser(user);
        entity.setRawJson(rawJson);
        entity.setUpdatedAt(Instant.now());

        if (profile.getFinancialProfile() != null) {
            entity.setPersonalityType(profile.getFinancialProfile().getPersonalityType());
            entity.setReasoning(profile.getFinancialProfile().getReasoning());
        }
        if (profile.getPeriod() != null) {
            entity.setPeriodStart(profile.getPeriod().getStartDate());
            entity.setPeriodEnd(profile.getPeriod().getEndDate());
        }
        if (profile.getTotals() != null) {
            entity.setCurrency(profile.getTotals().getCurrency());
            entity.setTotalIncome(BigDecimal.valueOf(profile.getTotals().getTotalIncome()));
            entity.setTotalExpense(BigDecimal.valueOf(profile.getTotals().getTotalExpense()));
        }
        if (profile.getRecommendedFreePocketPct() != null) {
            double pct = profile.getRecommendedFreePocketPct();
            if (pct < 0) pct = 0;
            if (pct > 50) pct = 50;
            entity.setRecommendedFreePocketPct(pct);
        }

        profileAnalyzedAIRepository.save(entity);

        redisService.save(RedisService.aiProfileKey(username), rawJson, AI_PROFILE_TTL);
        redisService.save(RedisService.aiCooldownKey(username), "1", AI_COOLDOWN_TTL);
    }

    private String sanitizeJsonResponse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new RuntimeException("AI вернул пустой ответ");
        }

        String text = raw.trim();
        Matcher fenceMatcher = CODE_FENCE.matcher(text);
        if (fenceMatcher.find()) {
            text = fenceMatcher.group(1).trim();
        }

        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1);
        }

        return text;
    }
}
