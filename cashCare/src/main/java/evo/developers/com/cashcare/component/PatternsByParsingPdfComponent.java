package evo.developers.com.cashcare.component;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class PatternsByParsingPdfComponent {
    public static class Tbank {
        public static final String START_TEXT = "Движение средств за период";
        public static final String END_TEXT = "Пополнения:";

        public static final Pattern DATE_START_PATTERN = Pattern.compile("^\\d{2}\\.\\d{2}\\.\\d{4}");
        public static final List<String> IGNORE_KEYWORDS = Arrays.asList("АО «ТБанк»", "БИК", "ИНН", "КПП", "лицензия");

        public static final Pattern LINE_PATTERN = Pattern.compile(
                "(?<date1>\\d{2}\\.\\d{2}\\.\\d{4})" +
                        "(?:\\s+(?<time1>\\d{2}:\\d{2}))?" +
                        "\\s+(?<date2>\\d{2}\\.\\d{2}\\.\\d{4})" +
                        "(?:\\s+(?<time2>\\d{2}:\\d{2}))?" +
                        "\\s+(?<amount1>[+-]?\\d{1,3}(?: \\d{3})*(?:[.,]\\d+)?\\s*₽)" +
                        "\\s+(?<amount2>[+-]?\\d{1,3}(?: \\d{3})*(?:[.,]\\d+)?\\s*₽)" +
                        "\\s+(?<description>.+)$"
        );

        public static final List<Pattern> STRICT_CARD_PATTERNS = Arrays.asList(
                Pattern.compile("на\\s+(\\d{4})\\b", Pattern.CASE_INSENSITIVE),
                Pattern.compile("по\\s+(\\d{4})\\b", Pattern.CASE_INSENSITIVE),
                Pattern.compile("для\\s+(\\d{4})\\b", Pattern.CASE_INSENSITIVE),
                Pattern.compile("с\\s+(\\d{4})\\b", Pattern.CASE_INSENSITIVE)
        );

        public static final Pattern LOOSE_CARD_PATTERN = Pattern.compile("\\b(\\d{4})\\b");
        public static final Pattern GARBAGE_PATTERN = Pattern.compile("\\s+\\d+\\s+Дата и время.*$", Pattern.CASE_INSENSITIVE);
        public static final Pattern TIME_PATTERN = Pattern.compile("\\b\\d{2}:\\d{2}\\b");
        public static final Pattern SPACES_PATTERN = Pattern.compile("\\s+");
    }
}
