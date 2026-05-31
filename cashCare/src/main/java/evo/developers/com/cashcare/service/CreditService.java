package evo.developers.com.cashcare.service;

import evo.developers.com.cashcare.dto.request.CreditRequest;
import evo.developers.com.cashcare.dto.response.CreditResponse;
import evo.developers.com.cashcare.entity.CreditEntity;
import evo.developers.com.cashcare.entity.UserEntity;
import evo.developers.com.cashcare.exception.NotFoundException;
import evo.developers.com.cashcare.exception.ValidInputException;
import evo.developers.com.cashcare.jpa.CreditRepository;
import evo.developers.com.cashcare.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditService {

    private final UserRepository userRepository;
    private final CreditRepository creditRepository;

    @Transactional(readOnly = true)
    public List<CreditResponse> listCredits(String username) throws NotFoundException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return creditRepository.findAllByUserOrderByIdAsc(user)
                .stream()
                .map(CreditResponse::from)
                .toList();
    }

    @Transactional
    public CreditResponse createCredit(String username, CreditRequest req) throws NotFoundException, ValidInputException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        validate(req);

        CreditEntity entity = new CreditEntity();
        entity.setUser(user);
        applyRequest(entity, req);
        creditRepository.save(entity);
        return CreditResponse.from(entity);
    }

    @Transactional
    public CreditResponse updateCredit(String username, Long id, CreditRequest req) throws NotFoundException, ValidInputException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        CreditEntity entity = creditRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Credit not found"));
        validate(req);

        applyRequest(entity, req);
        creditRepository.save(entity);
        return CreditResponse.from(entity);
    }

    @Transactional
    public void deleteCredit(String username, Long id) throws NotFoundException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        CreditEntity entity = creditRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Credit not found"));
        creditRepository.delete(entity);
    }

    /**
     * Bulk replace: удалить все и заменить переданным списком (используется в init-модалке).
     */
    @Transactional
    public List<CreditResponse> replaceAll(String username, List<CreditRequest> requests) throws NotFoundException, ValidInputException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        List<CreditRequest> safeList = requests == null ? List.of() : requests;
        for (CreditRequest req : safeList) {
            validate(req);
        }

        creditRepository.deleteByUser(user);

        List<CreditResponse> result = new ArrayList<>(safeList.size());
        for (CreditRequest req : safeList) {
            CreditEntity entity = new CreditEntity();
            entity.setUser(user);
            applyRequest(entity, req);
            creditRepository.save(entity);
            result.add(CreditResponse.from(entity));
        }
        return result;
    }

    private void applyRequest(CreditEntity entity, CreditRequest req) {
        entity.setName(req.getName().trim());
        entity.setType(req.getType());
        entity.setBalance(req.getBalance() != null ? req.getBalance() : BigDecimal.ZERO);
        entity.setMonthlyPayment(req.getMonthlyPayment() != null ? req.getMonthlyPayment() : BigDecimal.ZERO);
        entity.setInterestRate(req.getInterestRate());
        entity.setMonthsLeft(req.getMonthsLeft());
    }

    private void validate(CreditRequest req) throws ValidInputException {
        if (req == null) {
            throw new ValidInputException("Empty credit", List.of("request is null"));
        }
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            throw new ValidInputException("Name is required", List.of("name blank"));
        }
        if (req.getType() == null) {
            throw new ValidInputException("Type is required", List.of("type null"));
        }
        if (req.getMonthlyPayment() == null || req.getMonthlyPayment().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidInputException("Monthly payment must be >= 0", List.of("monthlyPayment invalid"));
        }
        if (req.getBalance() == null || req.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidInputException("Balance must be >= 0", List.of("balance invalid"));
        }
    }
}
