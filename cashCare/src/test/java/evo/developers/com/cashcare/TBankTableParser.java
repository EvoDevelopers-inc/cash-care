package evo.developers.com.cashcare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import evo.developers.com.cashcare.model.CleanedDesc;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TBankTableParser {

    private static final String PDF_FILE = "/Users/artemzbaranskij/flutter/test2.pdf";
    private static final String START_TEXT = "Движение средств за период";
    private static final String END_TEXT = "Пополнения:";

    private static final Pattern DATE_START_PATTERN = Pattern.compile("^\\d{2}\\.\\d{2}\\.\\d{4}");
    private static final List<String> IGNORE_KEYWORDS = Arrays.asList("АО «ТБанк»", "БИК", "ИНН", "КПП", "лицензия");

    // Главный паттерн разбора строки транзакции
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "(?<date1>\\d{2}\\.\\d{2}\\.\\d{4})" +
            "(?:\\s+(?<time1>\\d{2}:\\d{2}))?" +
            "\\s+(?<date2>\\d{2}\\.\\d{2}\\.\\d{4})" +
            "(?:\\s+(?<time2>\\d{2}:\\d{2}))?" +
            "\\s+(?<amount1>[+-]?\\d{1,3}(?: \\d{3})*(?:[.,]\\d+)?\\s*₽)" +
            "\\s+(?<amount2>[+-]?\\d{1,3}(?: \\d{3})*(?:[.,]\\d+)?\\s*₽)" +
            "\\s+(?<description>.+)$"
    );

    // Строгие паттерны поиска карт
    private static final List<Pattern> STRICT_CARD_PATTERNS = Arrays.asList(
            Pattern.compile("на\\s+(\\d{4})\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("по\\s+(\\d{4})\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("для\\s+(\\d{4})\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("с\\s+(\\d{4})\\b", Pattern.CASE_INSENSITIVE)
    );

    private static final Pattern LOOSE_CARD_PATTERN = Pattern.compile("\\b(\\d{4})\\b");
    private static final Pattern GARBAGE_PATTERN = Pattern.compile("\\s+\\d+\\s+Дата и время.*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_PATTERN = Pattern.compile("\\b\\d{2}:\\d{2}\\b");
    private static final Pattern SPACES_PATTERN = Pattern.compile("\\s+");

    public static void main(String[] args) {
        try {
            List<BankTransaction> data = extractTableFromPdf(PDF_FILE);

            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(new File("output.json"), data);

            System.out.println("✅ Извлечено " + data.size() + " транзакций");

            for (int i = 0; i <  data.size(); i++) {
                BankTransaction t = data.get(i);
                System.out.printf("%d. '%s' | %s | %s \n", i + 1, t.getDescription(), t.getCardNum(), t.getAmountInOpCurrency());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<BankTransaction> extractTableFromPdf(String pdfPath) throws IOException {
        List<BankTransaction> rows = new ArrayList<>();
        List<String> buffer = new ArrayList<>();
        boolean capture = false;

        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            for (String line : text.split("\\r?\\n")) {
                if (line.contains(START_TEXT)) {
                    capture = true;
                    continue;
                }
                if (line.contains(END_TEXT)) {
                    capture = false;
                    break;
                }
                if (capture) {
                    boolean shouldIgnore = IGNORE_KEYWORDS.stream().anyMatch(line::contains);
                    if (shouldIgnore) {
                        continue;
                    }
                    buffer.add(line.trim());
                }
            }
        }

        List<String> mergedLines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String line : buffer) {
            if (DATE_START_PATTERN.matcher(line).find()) {
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
            Matcher match = LINE_PATTERN.matcher(line);
            if (match.matches()) {
                String rawDescription = match.group("description");
                CleanedDesc cleaned = cleanDescription(rawDescription);

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
            } else {
                System.out.println("⚠ Не удалось разобрать: " + line);
            }
        }

        return rows;
    }

    public static CleanedDesc cleanDescription(String description) {
        String desc = description.trim();
        String cardNum = "-";

        desc = GARBAGE_PATTERN.matcher(desc).replaceAll("");

        for (Pattern pattern : STRICT_CARD_PATTERNS) {
            Matcher matcher = pattern.matcher(desc);
            if (matcher.find()) {
                cardNum = matcher.group(1);
                break;
            }
        }

        if ("-".equals(cardNum)) {
            Matcher looseMatcher = LOOSE_CARD_PATTERN.matcher(desc);
            if (looseMatcher.find()) {
                cardNum = looseMatcher.group(1);
            }
        }

        if (!"-".equals(cardNum)) {
            desc = desc.replaceAll("\\b" + cardNum + "\\b", "");
        }

        desc = TIME_PATTERN.matcher(desc).replaceAll("");

        desc = SPACES_PATTERN.matcher(desc).replaceAll(" ").trim();

        return new CleanedDesc(desc, cardNum);
    }
}
