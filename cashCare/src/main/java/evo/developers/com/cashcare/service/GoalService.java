package evo.developers.com.cashcare.service;

import evo.developers.com.cashcare.dto.request.GoalContributionRequest;
import evo.developers.com.cashcare.dto.request.GoalRequest;
import evo.developers.com.cashcare.dto.response.GoalResponse;
import evo.developers.com.cashcare.entity.GoalContributionEntity;
import evo.developers.com.cashcare.entity.GoalEntity;
import evo.developers.com.cashcare.entity.UserEntity;
import evo.developers.com.cashcare.exception.NotFoundException;
import evo.developers.com.cashcare.exception.ValidInputException;
import evo.developers.com.cashcare.jpa.GoalContributionRepository;
import evo.developers.com.cashcare.jpa.GoalRepository;
import evo.developers.com.cashcare.jpa.UserRepository;
import evo.developers.com.cashcare.model.GoalCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoalService {

    private static final int MAX_GOALS_PER_USER = 20;
    private static final Duration CONTRIBUTION_LOCK = Duration.ofDays(30);
    private static final ZoneId ZONE = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter HUMAN_NEXT = DateTimeFormatter
            .ofPattern("d MMMM, HH:mm", new Locale("ru"));

    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final GoalContributionRepository contributionRepository;

    @Transactional(readOnly = true)
    public List<GoalResponse> listGoals(String username) throws NotFoundException {
        UserEntity user = getUser(username);
        return goalRepository.findAllByUserOrderByCreatedAtAsc(user)
                .stream()
                .map((g) -> GoalResponse.from(g, contributionRepository.findAllByGoalOrderByCreatedAtDesc(g)))
                .toList();
    }

    @Transactional
    public GoalResponse createGoal(String username, GoalRequest req)
            throws NotFoundException, ValidInputException {
        UserEntity user = getUser(username);
        validate(req);

        if (goalRepository.countByUser(user) >= MAX_GOALS_PER_USER) {
            throw new ValidInputException(
                    "Лимит — 20 целей. Закрой или удали старую, чтобы добавить новую.",
                    List.of("limit reached")
            );
        }

        GoalEntity entity = new GoalEntity();
        entity.setUser(user);
        applyRequest(entity, req);

        if (req.getSavedAmount() != null && req.getSavedAmount().compareTo(BigDecimal.ZERO) > 0) {
            entity.setSavedAmount(min(req.getSavedAmount(), entity.getTargetAmount()));
        } else {
            entity.setSavedAmount(BigDecimal.ZERO);
        }
        markCompletedIfReached(entity);

        goalRepository.save(entity);
        return GoalResponse.from(entity);
    }

    @Transactional
    public GoalResponse updateGoal(String username, Long id, GoalRequest req)
            throws NotFoundException, ValidInputException {
        UserEntity user = getUser(username);
        GoalEntity entity = goalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Goal not found"));
        validate(req);

        applyRequest(entity, req);

        if (req.getSavedAmount() != null) {
            BigDecimal saved = req.getSavedAmount();
            if (saved.compareTo(BigDecimal.ZERO) < 0) saved = BigDecimal.ZERO;
            entity.setSavedAmount(min(saved, entity.getTargetAmount()));
        } else if (entity.getSavedAmount() != null
                && entity.getSavedAmount().compareTo(entity.getTargetAmount()) > 0) {
            entity.setSavedAmount(entity.getTargetAmount());
        }
        markCompletedIfReached(entity);

        goalRepository.save(entity);
        return GoalResponse.from(entity);
    }

    @Transactional
    public void deleteGoal(String username, Long id) throws NotFoundException {
        UserEntity user = getUser(username);
        GoalEntity entity = goalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Goal not found"));
        contributionRepository.deleteByGoal(entity);
        goalRepository.delete(entity);
    }

    @Transactional
    public GoalResponse contribute(String username, Long goalId, GoalContributionRequest req)
            throws NotFoundException, ValidInputException {
        UserEntity user = getUser(username);
        GoalEntity goal = goalRepository.findByIdAndUser(goalId, user)
                .orElseThrow(() -> new NotFoundException("Goal not found"));

        if (req == null || req.getAmount() == null
                || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidInputException(
                    "Сумма должна быть больше нуля",
                    List.of("amount invalid")
            );
        }

        Instant now = Instant.now();
        Instant windowStart = now.minus(CONTRIBUTION_LOCK);
        Optional<GoalContributionEntity> existing = contributionRepository
                .findTopByGoalAndCreatedAtBetweenOrderByCreatedAtDesc(goal, windowStart, now);
        if (existing.isPresent()) {
            Instant nextAt = existing.get().getCreatedAt().plus(CONTRIBUTION_LOCK);
            String nextHuman = nextAt.atZone(ZONE).format(HUMAN_NEXT);
            throw new ValidInputException(
                    "В этот раз уже откладывали на эту цель. Следующий взнос — " + nextHuman + ".",
                    List.of("monthly contribution lock")
            );
        }

        BigDecimal current = goal.getSavedAmount() != null ? goal.getSavedAmount() : BigDecimal.ZERO;
        BigDecimal newSaved = current.add(req.getAmount());

        if (goal.getTargetAmount() != null
                && newSaved.compareTo(goal.getTargetAmount()) > 0) {
            newSaved = goal.getTargetAmount();
        }
        goal.setSavedAmount(newSaved);
        markCompletedIfReached(goal);
        goalRepository.save(goal);

        GoalContributionEntity contribution = new GoalContributionEntity();
        contribution.setGoal(goal);
        contribution.setAmount(req.getAmount());
        if (req.getNote() != null && !req.getNote().isBlank()) {
            contribution.setNote(req.getNote().trim());
        }
        contributionRepository.save(contribution);

        return GoalResponse.from(goal, contributionRepository.findAllByGoalOrderByCreatedAtDesc(goal));
    }

    private UserEntity getUser(String username) throws NotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private void applyRequest(GoalEntity entity, GoalRequest req) {
        entity.setTitle(req.getTitle().trim());
        entity.setCategory(req.getCategory());

        if (req.getCategory() == GoalCategory.CUSTOM
                && req.getCustomEmoji() != null
                && !req.getCustomEmoji().isBlank()) {
            entity.setCustomEmoji(req.getCustomEmoji().trim());
        } else {
            entity.setCustomEmoji(null);
        }

        entity.setTargetAmount(req.getTargetAmount());
        entity.setTargetDate(req.getTargetDate());
    }

    private void markCompletedIfReached(GoalEntity entity) {
        BigDecimal target = entity.getTargetAmount();
        BigDecimal saved = entity.getSavedAmount();
        if (target == null || saved == null) return;

        if (saved.compareTo(target) >= 0 && entity.getCompletedAt() == null) {
            entity.setCompletedAt(Instant.now());
        } else if (saved.compareTo(target) < 0) {
            entity.setCompletedAt(null);
        }
    }

    private void validate(GoalRequest req) throws ValidInputException {
        if (req == null) {
            throw new ValidInputException("Empty goal", List.of("request null"));
        }
        if (req.getTitle() == null || req.getTitle().trim().isEmpty()) {
            throw new ValidInputException("Название цели обязательно", List.of("title blank"));
        }
        if (req.getCategory() == null) {
            throw new ValidInputException("Категория обязательна", List.of("category null"));
        }
        if (req.getTargetAmount() == null
                || req.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidInputException(
                    "Целевая сумма должна быть больше нуля",
                    List.of("targetAmount invalid")
            );
        }
        if (req.getCategory() == GoalCategory.CUSTOM) {
            String emoji = req.getCustomEmoji();
            if (emoji != null && emoji.length() > 8) {
                throw new ValidInputException(
                        "Эмодзи слишком длинный",
                        List.of("customEmoji too long")
                );
            }
        }
    }

    private BigDecimal min(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.compareTo(b) <= 0 ? a : b;
    }

}
