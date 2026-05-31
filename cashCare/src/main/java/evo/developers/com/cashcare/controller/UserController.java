package evo.developers.com.cashcare.controller;

import evo.developers.com.cashcare.dto.request.UserSurveyRequest;
import evo.developers.com.cashcare.dto.response.UserSurveyResponse;
import evo.developers.com.cashcare.exception.BaseException;
import evo.developers.com.cashcare.security.UserPrincipal;
import evo.developers.com.cashcare.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@RequestMapping("/api/user")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/survey")
    public ResponseEntity<UserSurveyResponse> getSurvey(@AuthenticationPrincipal UserPrincipal principal)
            throws BaseException {
        return ResponseEntity.ok(userService.getSurvey(principal.getUsername()));
    }

    @PostMapping("/survey")
    public ResponseEntity<UserSurveyResponse> saveSurvey(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserSurveyRequest request
    ) throws BaseException {
        return ResponseEntity.ok(userService.saveSurvey(principal.getUsername(), request));
    }
}
