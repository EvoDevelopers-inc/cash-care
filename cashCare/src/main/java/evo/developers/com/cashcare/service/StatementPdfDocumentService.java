package evo.developers.com.cashcare.service;

import evo.developers.com.cashcare.component.JsonHelper;
import evo.developers.com.cashcare.config.UploadProperties;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatementPdfDocumentService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final Log log = LogFactory.getLog(StatementPdfDocumentService.class);

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    );

    private final UserRepository userRepository;
    private final PdfParserService pdfParserService;
    private final UploadProperties uploadProperties;
    private final JsonHelper jsonHelper;
    private final AiAnalyzeService aiAnalyzeService;

    public AnalyzeAiProfile processAiAnalyzeStatement(String username, MultipartFile file) throws BaseException {
        validatePdf(file);

        userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        byte[] pdfBytes = readBytes(file);
        List<BankTransaction> allTransactions = parseStatement(pdfBytes);
        List<BankTransaction> transactions = filterLastMonth(allTransactions);

        log.info("Statement parsed for " + username
                + ": total tx=" + allTransactions.size()
                + ", kept last month=" + transactions.size());

        String payload = jsonHelper.toJson(transactions);
        AnalyzeAiProfile profile = aiAnalyzeService.analyzeTransaction(username, payload);
        log.info("AI profile analyzed for user " + username + ": " + jsonHelper.toJson(profile));

        return profile;
    }

    public List<BankTransaction> parseStatement(byte[] pdfBytes) throws ParsingPdfException {
        return pdfParserService.parseTBankPdf(pdfBytes);
    }

    /**
     * Keep only transactions from the latest calendar month present in the statement.
     * If dates can't be parsed at all — return everything as-is.
     */
    List<BankTransaction> filterLastMonth(List<BankTransaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return transactions == null ? List.of() : transactions;
        }

        List<Dated> dated = new ArrayList<>(transactions.size());
        for (BankTransaction tx : transactions) {
            LocalDate date = parseDate(tx.getDateTimeOperation());
            if (date == null) {
                date = parseDate(tx.getDateTimeWriteOff());
            }
            dated.add(new Dated(tx, date));
        }

        boolean anyDated = dated.stream().anyMatch(d -> d.date != null);
        if (!anyDated) {
            return transactions;
        }

        YearMonth lastMonth = dated.stream()
                .map(d -> d.date)
                .filter(d -> d != null)
                .map(YearMonth::from)
                .max(Comparator.naturalOrder())
                .orElse(null);

        if (lastMonth == null) {
            return transactions;
        }

        List<BankTransaction> result = new ArrayList<>();
        for (Dated d : dated) {
            if (d.date != null && YearMonth.from(d.date).equals(lastMonth)) {
                result.add(d.tx);
            }
        }
        return result.isEmpty() ? transactions : result;
    }

    private LocalDate parseDate(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;

        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDateTime.parse(trimmed, fmt).toLocalDate();
            } catch (Exception ignored) {
            }
            try {
                return LocalDate.parse(trimmed, fmt);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private record Dated(BankTransaction tx, LocalDate date) {
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
