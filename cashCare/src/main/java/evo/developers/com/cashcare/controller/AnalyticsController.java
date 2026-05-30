package evo.developers.com.cashcare.controller;

import evo.developers.com.cashcare.dto.response.AnalyticsOverviewResponse;
import evo.developers.com.cashcare.exception.BaseException;
import evo.developers.com.cashcare.security.UserPrincipal;
import evo.developers.com.cashcare.service.AnalyticsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewResponse> overview(
            @AuthenticationPrincipal UserPrincipal principal
    ) throws BaseException {
        return ResponseEntity.ok(analyticsService.buildOverview(principal.getUsername()));
    }

    @PostMapping("/ai-budget")
    public ResponseEntity<AnalyticsOverviewResponse> runBudgetAi(
            @AuthenticationPrincipal UserPrincipal principal
    ) throws BaseException {
        analyticsService.runBudgetAnalysis(principal.getUsername());
        return ResponseEntity.ok(analyticsService.buildOverview(principal.getUsername()));
    }
}
