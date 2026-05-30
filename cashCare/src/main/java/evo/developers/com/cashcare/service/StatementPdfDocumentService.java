package evo.developers.com.cashcare.service;

import evo.developers.com.cashcare.component.JsonHelper;
import evo.developers.com.cashcare.config.UploadProperties;
import evo.developers.com.cashcare.entity.UserEntity;
import evo.developers.com.cashcare.exception.BaseException;
import evo.developers.com.cashcare.exception.NotFoundException;
import evo.developers.com.cashcare.exception.ParsingPdfException;
import evo.developers.com.cashcare.exception.ValidInputException;
import evo.developers.com.cashcare.jpa.UserRepository;
import evo.developers.com.cashcare.model.AnalyzeAiProfile;
import evo.developers.com.cashcare.model.BankTransaction;
import lombok.RequiredArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatementPdfDocumentService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final Log log = LogFactory.getLog(StatementPdfDocumentService.class);

    private final UserRepository userRepository;
    private final PdfParserService pdfParserService;
    private final UploadProperties uploadProperties;
    private final JsonHelper jsonHelper;
    private final AiAnalyzeService aiAnalyzeService;
    private final RedisService redisService;

    public AnalyzeAiProfile processAiAnalyzeStatement(String username, MultipartFile file) throws BaseException {
        validatePdf(file);

        userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        byte[] pdfBytes = readBytes(file);
        List<BankTransaction> transactions = parseStatement(pdfBytes);

        String payload = jsonHelper.toJson(transactions);
        AnalyzeAiProfile profile = aiAnalyzeService.analyzeTransaction(username, payload);
        log.info("AI profile analyzed for user " + username + ": " + jsonHelper.toJson(profile));

        redisService.save(username, jsonHelper.toJson(profile)); // TODO SAVE DATABASE!

        return profile;
    }

    public List<BankTransaction> parseStatement(byte[] pdfBytes) throws ParsingPdfException {
        return pdfParserService.parseTBankPdf(pdfBytes);
    }

    private void validatePdf(MultipartFile file) throws ValidInputException {
        if (file == null || file.isEmpty()) {
            throw new ValidInputException("PDF file is required", List.of("file is empty"));
        }

        if (file.getSize() > uploadProperties.getMaxSizeBytes()) {
            throw new ValidInputException(
                    "File is too large",
                    List.of("max size: " + uploadProperties.getMaxSizeBytes() + " bytes")
            );
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        boolean isPdfByType = PDF_CONTENT_TYPE.equalsIgnoreCase(contentType);
        boolean isPdfByName = filename != null && filename.toLowerCase().endsWith(".pdf");

        if (!isPdfByType && !isPdfByName) {
            throw new ValidInputException("Only PDF files are allowed", List.of("invalid file type"));
        }
    }

    private byte[] readBytes(MultipartFile file) throws ValidInputException {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ValidInputException("Failed to read uploaded file", List.of(e.getMessage()));
        }
    }
}
