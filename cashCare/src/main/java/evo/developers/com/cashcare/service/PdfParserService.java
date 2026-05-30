package evo.developers.com.cashcare.service;

import evo.developers.com.cashcare.component.PdfParserTBankComponent;
import evo.developers.com.cashcare.exception.ParsingPdfException;
import evo.developers.com.cashcare.model.BankTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class PdfParserService {

    private final PdfParserTBankComponent pdfParserTBank;

    public List<BankTransaction> parseTBankPdf(byte[] pdf) throws ParsingPdfException {
        return pdfParserTBank.extractTableFromTBankPdf(pdf);
    }

}
