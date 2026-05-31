package evo.developers.com.cashcare.controller;

import evo.developers.com.cashcare.dto.request.CreditRequest;
import evo.developers.com.cashcare.dto.response.CreditResponse;
import evo.developers.com.cashcare.exception.BaseException;
import evo.developers.com.cashcare.security.UserPrincipal;
import evo.developers.com.cashcare.service.CreditService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CreditController {

    private final CreditService creditService;

    @GetMapping
    public ResponseEntity<List<CreditResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal
    ) throws BaseException {
        return ResponseEntity.ok(creditService.listCredits(principal.getUsername()));
    }

    @PostMapping
    public ResponseEntity<CreditResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreditRequest request
    ) throws BaseException {
        return ResponseEntity.ok(creditService.createCredit(principal.getUsername(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CreditResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CreditRequest request
    ) throws BaseException {
        return ResponseEntity.ok(creditService.updateCredit(principal.getUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) throws BaseException {
        creditService.deleteCredit(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Bulk replace — для init-модалки. Передаём массив, всё перезаписывается.
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<CreditResponse>> bulkReplace(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody List<CreditRequest> requests
    ) throws BaseException {
        return ResponseEntity.ok(creditService.replaceAll(principal.getUsername(), requests));
    }
}
