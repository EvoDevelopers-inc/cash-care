package evo.developers.com.cashcare.controller;

import evo.developers.com.cashcare.dto.request.SpontaneousTransactionRequest;
import evo.developers.com.cashcare.dto.response.SpontaneousTransactionResponse;
import evo.developers.com.cashcare.exception.BaseException;
import evo.developers.com.cashcare.security.UserPrincipal;
import evo.developers.com.cashcare.service.SpontaneousTransactionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finances/spontaneous")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class SpontaneousTransactionController {

    private final SpontaneousTransactionService spontaneousService;

    @GetMapping
    public ResponseEntity<List<SpontaneousTransactionResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long monthlyFinancesId
    ) throws BaseException {
        return ResponseEntity.ok(
                spontaneousService.listForMonth(principal.getUsername(), monthlyFinancesId)
        );
    }

    @PostMapping
    public ResponseEntity<SpontaneousTransactionResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SpontaneousTransactionRequest request
    ) throws BaseException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(spontaneousService.create(principal.getUsername(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) throws BaseException {
        spontaneousService.delete(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
