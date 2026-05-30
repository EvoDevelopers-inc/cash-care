package evo.developers.com.cashcare.controller;

import evo.developers.com.cashcare.dto.request.CreateMonthlyFinancesRequest;
import evo.developers.com.cashcare.dto.request.InitUserRequest;
import evo.developers.com.cashcare.dto.request.UpdateMonthlyFinancesRequest;
import evo.developers.com.cashcare.dto.response.InitSetupResponse;
import evo.developers.com.cashcare.dto.response.MonthlyFinancesResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/finances/monthly")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MonthlyFinancesController {

    private final FinancesService financesService;

    @GetMapping
    public ResponseEntity<List<MonthlyFinancesResponse>> list(@AuthenticationPrincipal UserPrincipal principal)
            throws BaseException {
        return ResponseEntity.ok(financesService.getMonthlyFinancesList(principal.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonthlyFinancesResponse> get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) throws BaseException {
        return ResponseEntity.ok(financesService.getMonthlyFinances(principal.getUsername(), id));
    }

    @GetMapping("/init/setup")
    public ResponseEntity<InitSetupResponse> getInitSetup(@AuthenticationPrincipal UserPrincipal principal)
            throws BaseException {
        return ResponseEntity.ok(financesService.getInitSetup(principal.getUsername()));
    }

    @PostMapping("/init")
    public ResponseEntity<MonthlyFinancesResponse> init(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody InitUserRequest request
    ) throws BaseException {
        return ResponseEntity.ok(financesService.initMonthlyFinances(principal.getUsername(), request));
    }

    @PostMapping
    public ResponseEntity<MonthlyFinancesResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateMonthlyFinancesRequest request
    ) throws BaseException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(financesService.createMonthlyFinances(principal.getUsername(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MonthlyFinancesResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateMonthlyFinancesRequest request
    ) throws BaseException {
        return ResponseEntity.ok(financesService.updateMonthlyFinances(principal.getUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) throws BaseException {
        financesService.deleteMonthlyFinances(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
