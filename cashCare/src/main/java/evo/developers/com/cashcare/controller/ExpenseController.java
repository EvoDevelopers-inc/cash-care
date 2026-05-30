package evo.developers.com.cashcare.controller;

import evo.developers.com.cashcare.dto.request.CreateExpenseRequest;
import evo.developers.com.cashcare.dto.request.UpdateExpenseRequest;
import evo.developers.com.cashcare.dto.response.ExpenseResponse;
import evo.developers.com.cashcare.exception.BaseException;
import evo.developers.com.cashcare.security.UserPrincipal;
import evo.developers.com.cashcare.service.FinancesService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/finances/expenses")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ExpenseController {

    private final FinancesService financesService;

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long monthlyFinancesId,
            @RequestParam(required = false) Long categoryId
    ) throws BaseException {
        return ResponseEntity.ok(
                financesService.getExpenses(principal.getUsername(), monthlyFinancesId, categoryId)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) throws BaseException {
        return ResponseEntity.ok(financesService.getExpense(principal.getUsername(), id));
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateExpenseRequest request
    ) throws BaseException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(financesService.createExpense(principal.getUsername(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateExpenseRequest request
    ) throws BaseException {
        return ResponseEntity.ok(financesService.updateExpense(principal.getUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) throws BaseException {
        financesService.deleteExpense(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
