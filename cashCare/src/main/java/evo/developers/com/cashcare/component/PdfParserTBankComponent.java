package evo.developers.com.cashcare.component;

import evo.developers.com.cashcare.exception.ParsingPdfException;
import evo.developers.com.cashcare.model.BankTransaction;
import evo.developers.com.cashcare.model.CleanedDesc;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PdfParserTBankComponent {

    public List<BankTransaction> extractTableFromTBankPdf(byte[] pdf) throws ParsingPdfException {
        List<BankTransaction> rows = new ArrayList<>();
        List<String> buffer = new ArrayList<>();
        boolean capture = false;

        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            for (String line : text.split("\\r?\\n")) {
                if (line.contains(PatternsByParsingPdfComponent.Tbank.START_TEXT)) {
                    capture = true;
                    continue;
                }
                if (line.contains(PatternsByParsingPdfComponent.Tbank.END_TEXT)) {
                    capture = false;
                    break;
                }
                if (capture) {
                    boolean shouldIgnore = PatternsByParsingPdfComponent.Tbank.IGNORE_KEYWORDS.stream().anyMatch(line::contains);
                    if (shouldIgnore) {
                        continue;
                    }
                    buffer.add(line.trim());
                }
            }
        }catch (Exception e){
            throw new ParsingPdfException("Error parsing pdf!", List.of(e.getMessage()));
        }

        List<String> mergedLines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String line : buffer) {
            if (PatternsByParsingPdfComponent.Tbank.DATE_START_PATTERN.matcher(line).find()) {
                if (currentLine.length() > 0) {
                    mergedLines.add(currentLine.toString().trim());
                }
                currentLine = new StringBuilder(line);
            } else {
                currentLine.append(" ").append(line);
            }
        }
        if (currentLine.length() > 0) {
            mergedLines.add(currentLine.toString().trim());
        }

        for (String line : mergedLines) {
            Matcher match = PatternsByParsingPdfComponent.Tbank.LINE_PATTERN.matcher(line);
            if (match.matches()) {
                String rawDescription = match.group("description");
                CleanedDesc cleaned = cleanDescriptionTBank(rawDescription);

                BankTransaction row = new BankTransaction();

                String time1 = match.group("time1") != null ? match.group("time1") : "";
                row.setDateTimeOperation((match.group("date1") + " " + time1).trim());

                String time2 = match.group("time2") != null ? match.group("time2") : "";
                row.setDateTimeWriteOff((match.group("date2") + " " + time2).trim());

                row.setAmountInOpCurrency(match.group("amount1"));
                row.setAmountInCardCurrency(match.group("amount2"));
                row.setDescription(cleaned.getDescription());
                row.setCardNum(cleaned.getCardNum());

                rows.add(row);
            }
        }

        return rows;
    }

    public CleanedDesc cleanDescriptionTBank(String description) {
        String desc = description.trim();
        String cardNum = "-";

        desc = PatternsByParsingPdfComponent.Tbank.GARBAGE_PATTERN.matcher(desc).replaceAll("");

        for (Pattern pattern : PatternsByParsingPdfComponent.Tbank.STRICT_CARD_PATTERNS) {
            Matcher matcher = pattern.matcher(desc);
            if (matcher.find()) {
                cardNum = matcher.group(1);
                break;
            }
        }

        if ("-".equals(cardNum)) {
            Matcher looseMatcher = PatternsByParsingPdfComponent.Tbank.LOOSE_CARD_PATTERN.matcher(desc);
            if (looseMatcher.find()) {
                cardNum = looseMatcher.group(1);
            }
        }

        if (!"-".equals(cardNum)) {
            desc = desc.replaceAll("\\b" + cardNum + "\\b", "");
        }

        desc = PatternsByParsingPdfComponent.Tbank.TIME_PATTERN.matcher(desc).replaceAll("");

        desc = PatternsByParsingPdfComponent.Tbank.SPACES_PATTERN.matcher(desc).replaceAll(" ").trim();

        return new CleanedDesc(desc, cardNum);
    }
}
