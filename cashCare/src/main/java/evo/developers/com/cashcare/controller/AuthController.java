package evo.developers.com.cashcare.controller;

import evo.developers.com.cashcare.dto.base.Response;
import evo.developers.com.cashcare.dto.request.AuthRequest;
import evo.developers.com.cashcare.dto.request.RefreshTokenRequest;
import evo.developers.com.cashcare.dto.response.AuthResponse;
import evo.developers.com.cashcare.dto.response.UserProfileResponse;
import evo.developers.com.cashcare.exception.BaseException;
import evo.developers.com.cashcare.model.JwtToken;
import evo.developers.com.cashcare.security.UserPrincipal;
import evo.developers.com.cashcare.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) throws BaseException {
        JwtToken tokens = authService.login(request.getLogin(), request.getPassword());
        return ResponseEntity.ok(AuthResponse.from(tokens));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) throws BaseException {
        JwtToken tokens = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(AuthResponse.from(tokens));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserProfileResponse> me(@AuthenticationPrincipal UserPrincipal principal) throws BaseException {
        return ResponseEntity.ok(UserProfileResponse.from(authService.getCurrentUser(principal.getUsername())));
    }
}
