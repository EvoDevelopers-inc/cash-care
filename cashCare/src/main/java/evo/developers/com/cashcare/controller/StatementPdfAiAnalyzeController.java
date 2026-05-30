package evo.developers.com.cashcare.controller;

import evo.developers.com.cashcare.dto.response.StatementAiAnalyzeResponse;
import evo.developers.com.cashcare.exception.BaseException;
import evo.developers.com.cashcare.model.AnalyzeAiProfile;
import evo.developers.com.cashcare.security.UserPrincipal;
import evo.developers.com.cashcare.service.StatementPdfDocumentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/statements")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class StatementPdfAiAnalyzeController {

    private final StatementPdfDocumentService statementDocumentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StatementAiAnalyzeResponse> uploadPdfStatement(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file
    ) throws BaseException {
        AnalyzeAiProfile profile = statementDocumentService.processAiAnalyzeStatement(principal.getUsername(), file);
        return ResponseEntity.ok(StatementAiAnalyzeResponse.from(profile));
    }
}
