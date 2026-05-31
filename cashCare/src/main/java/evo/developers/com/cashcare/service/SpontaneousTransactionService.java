package evo.developers.com.cashcare.service;

import evo.developers.com.cashcare.dto.request.SpontaneousTransactionRequest;
import evo.developers.com.cashcare.dto.response.SpontaneousTransactionResponse;
import evo.developers.com.cashcare.entity.MonthlyFinances;
import evo.developers.com.cashcare.entity.SpontaneousTransactionEntity;
import evo.developers.com.cashcare.entity.UserEntity;
import evo.developers.com.cashcare.exception.NotFoundException;
import evo.developers.com.cashcare.exception.ValidInputException;
import evo.developers.com.cashcare.jpa.MonthlyFinancesRepository;
import evo.developers.com.cashcare.jpa.SpontaneousTransactionRepository;
import evo.developers.com.cashcare.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SpontaneousTransactionService {

    private static final ZoneId ZONE = ZoneId.of("Europe/Moscow");

    private final UserRepository userRepository;
    private final MonthlyFinancesRepository monthlyFinancesRepository;
    private final SpontaneousTransactionRepository spontaneousRepository;

    @Transactional(readOnly = true)
    public List<SpontaneousTransactionResponse> listForMonth(String username, Long monthlyFinancesId)
            throws NotFoundException, ValidInputException {
        MonthlyFinances mf = resolveMonth(username, monthlyFinancesId);
        ensureCalendarMonth(mf);
        YearMonth ym = YearMonth.of(mf.getYear(), mf.getMonth());
        Instant from = ym.atDay(1).atStartOfDay(ZONE).toInstant();
        Instant to = ym.plusMonths(1).atDay(1).atStartOfDay(ZONE).toInstant();
        return spontaneousRepository
                .findAllByMonthlyFinancesAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        mf, from, to
                )
                .stream()
                .map(SpontaneousTransactionResponse::from)
                .toList();
    }

    @Transactional
    public SpontaneousTransactionResponse create(String username, SpontaneousTransactionRequest req)
            throws NotFoundException, ValidInputException {
        validate(req);
        MonthlyFinances mf = resolveMonth(username, req.getMonthlyFinancesId());

        SpontaneousTransactionEntity entity = new SpontaneousTransactionEntity();
        entity.setMonthlyFinances(mf);
        entity.setType(req.getType());
        entity.setAmount(req.getAmount());
        if (req.getNote() != null && !req.getNote().isBlank()) {
            entity.setNote(req.getNote().trim());
        }
        spontaneousRepository.save(entity);
        return SpontaneousTransactionResponse.from(entity);
    }

    @Transactional
    public void delete(String username, Long id) throws NotFoundException {
        UserEntity user = getUser(username);
        MonthlyFinances mf = resolveCalendarMonthFinances(user)
                .orElseThrow(() -> new NotFoundException("Monthly finances not found"));
        SpontaneousTransactionEntity entity = spontaneousRepository
                .findByIdAndMonthlyFinances(id, mf)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
        spontaneousRepository.delete(entity);
    }

    private void validate(SpontaneousTransactionRequest req) throws ValidInputException {
        if (req == null) {
            throw new ValidInputException("Empty request", List.of("request null"));
        }
        if (req.getType() == null) {
            throw new ValidInputException("Укажи тип операции", List.of("type null"));
        }
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidInputException("Сумма должна быть больше нуля", List.of("amount invalid"));
        }
        if (req.getMonthlyFinancesId() == null) {
            throw new ValidInputException("Месяц не указан", List.of("monthlyFinancesId null"));
        }
    }

    private MonthlyFinances resolveMonth(String username, Long monthlyFinancesId) throws NotFoundException {
        UserEntity user = getUser(username);
        MonthlyFinances mf = monthlyFinancesRepository.findById(monthlyFinancesId)
                .orElseThrow(() -> new NotFoundException("Monthly finances not found"));
        if (mf.getUser().getId() != user.getId()) {
            throw new NotFoundException("Monthly finances not found");
        }
        return mf;
    }

    private Optional<MonthlyFinances> resolveCalendarMonthFinances(UserEntity user) {
        YearMonth ym = YearMonth.now(ZONE);
        return monthlyFinancesRepository.findByUserAndYearAndMonth(
                user, ym.getYear(), ym.getMonthValue()
        );
    }

    private void ensureCalendarMonth(MonthlyFinances mf) throws ValidInputException {
        YearMonth ym = YearMonth.now(ZONE);
        if (mf.getYear() != ym.getYear() || mf.getMonth() != ym.getMonthValue()) {
            throw new ValidInputException(
                    "Операции «вне плана» можно добавлять только в текущем календарном месяце",
                    List.of("not current calendar month")
            );
        }
    }

    private UserEntity getUser(String username) throws NotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
