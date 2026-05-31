package evo.developers.com.cashcare.controller;

import evo.developers.com.cashcare.dto.request.GoalContributionRequest;
import evo.developers.com.cashcare.dto.request.GoalRequest;
import evo.developers.com.cashcare.dto.response.GoalResponse;
import evo.developers.com.cashcare.exception.BaseException;
import evo.developers.com.cashcare.security.UserPrincipal;
import evo.developers.com.cashcare.service.GoalService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class GoalController {

    private final GoalService goalService;

    @GetMapping
    public ResponseEntity<List<GoalResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal
    ) throws BaseException {
        return ResponseEntity.ok(goalService.listGoals(principal.getUsername()));
    }

    @PostMapping
    public ResponseEntity<GoalResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GoalRequest request
    ) throws BaseException {
        return ResponseEntity.ok(goalService.createGoal(principal.getUsername(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody GoalRequest request
    ) throws BaseException {
        return ResponseEntity.ok(goalService.updateGoal(principal.getUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) throws BaseException {
        goalService.deleteGoal(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/contribute")
    public ResponseEntity<GoalResponse> contribute(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody GoalContributionRequest request
    ) throws BaseException {
        return ResponseEntity.ok(goalService.contribute(principal.getUsername(), id, request));
    }
}
