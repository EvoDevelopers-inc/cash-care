package evo.developers.com.cashcare.dto.response;

import evo.developers.com.cashcare.dto.base.Response;
import evo.developers.com.cashcare.model.AnalyzeAiProfile;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatementAiAnalyzeResponse extends Response {
    private AiAnalysisView analysis;

    public static StatementAiAnalyzeResponse from(AnalyzeAiProfile analysis) {
        StatementAiAnalyzeResponse response = new StatementAiAnalyzeResponse();
        response.setStatus(true);
        response.setMessage("AI analysis completed");
        response.setAnalysis(AiAnalysisView.from(analysis));
        return response;
    }
}
