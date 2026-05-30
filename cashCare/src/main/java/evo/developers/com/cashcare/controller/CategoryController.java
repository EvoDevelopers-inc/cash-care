package evo.developers.com.cashcare.controller;

import evo.developers.com.cashcare.dto.request.CreateCategoryRequest;
import evo.developers.com.cashcare.dto.request.UpdateCategoryRequest;
import evo.developers.com.cashcare.dto.response.CategoryResponse;
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
@RequestMapping("/api/finances/categories")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final FinancesService financesService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long monthlyFinancesId
    ) throws BaseException {
        return ResponseEntity.ok(financesService.getCategories(principal.getUsername(), monthlyFinancesId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) throws BaseException {
        return ResponseEntity.ok(financesService.getCategory(principal.getUsername(), id));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateCategoryRequest request
    ) throws BaseException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(financesService.createCategory(principal.getUsername(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) throws BaseException {
        return ResponseEntity.ok(financesService.updateCategory(principal.getUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) throws BaseException {
        financesService.deleteCategory(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
