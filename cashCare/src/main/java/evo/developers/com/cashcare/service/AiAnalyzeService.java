package evo.developers.com.cashcare.service;

import evo.developers.com.cashcare.component.JsonHelper;
import evo.developers.com.cashcare.component.OpenRouterClient;
import evo.developers.com.cashcare.component.PatternsByParsingPdfComponent;
import evo.developers.com.cashcare.exception.NotFoundException;
import evo.developers.com.cashcare.helper.TemplatePromptsAnalyzeAi;
import evo.developers.com.cashcare.model.AnalyzeAiProfile;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AiAnalyzeService {
    private final OpenRouterClient openRouterClient;
    private final JsonHelper jsonHelper;
    private final FinancesService financesService;

    public AnalyzeAiProfile analyzeTransaction(String username, String payload) throws NotFoundException {
        String response = openRouterClient.ask(
                TemplatePromptsAnalyzeAi.SYSTEM_PROMPT_ANALYZE_STATEMENT_PDF,
                payload
        );

        AnalyzeAiProfile profile = jsonHelper.fromJson(response, AnalyzeAiProfile.class);
        financesService.applyAiAnalysis(username, profile);
        return profile;
    }
}
