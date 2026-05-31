package evo.developers.com.cashcare.service;

import evo.developers.com.cashcare.entity.ProfileAnalyzedAIEntity;
import evo.developers.com.cashcare.entity.UserEntity;
import evo.developers.com.cashcare.exception.NotFoundException;
import evo.developers.com.cashcare.exception.ValidInputException;
import evo.developers.com.cashcare.jpa.ProfileAnalyzedAIRepository;
import evo.developers.com.cashcare.jpa.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Единый источник правды про заморозку бюджета и AI-кулдаун.
 * Бюджет фризится ровно на 30 дней с момента последнего AI-анализа.
 * Пока бюджет заморожен:
 *   - нельзя менять/удалять/создавать категории
 *   - нельзя менять зарплату в MonthlyFinances
 *   - нельзя запускать AI-обновление
 */
@Service
@RequiredArgsConstructor
public class BudgetLockService {

    public static final Duration LOCK_PERIOD = Duration.ofDays(30);
    private static final ZoneId ZONE = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter HUMAN = DateTimeFormatter
            .ofPattern("d MMMM, HH:mm")
            .withLocale(new Locale("ru"));

    private final RedisService redisService;
    private final UserRepository userRepository;
    private final ProfileAnalyzedAIRepository profileAnalyzedAIRepository;

    public LockState getLockState(String username) {
        UserEntity user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return LockState.unlocked();
        }
        return getLockState(user);
    }

    public LockState getLockState(UserEntity user) {
        String cooldownKey = RedisService.aiCooldownKey(user.getUsername());
        if (redisService.exists(cooldownKey)) {
            Long ttl = redisService.getTtlSeconds(cooldownKey);
            Instant unlockAt = ttl != null && ttl > 0
                    ? Instant.now().plusSeconds(ttl)
                    : Instant.now().plus(LOCK_PERIOD);
            return LockState.locked(unlockAt);
        }

        ProfileAnalyzedAIEntity entity = profileAnalyzedAIRepository.findByUser(user).orElse(null);
        if (entity == null || entity.getUpdatedAt() == null
                || entity.getRecommendedFreePocketPct() == null) {
            return LockState.unlocked();
        }

        Instant lastRun = entity.getUpdatedAt();
        Instant unlockAt = lastRun.plus(LOCK_PERIOD);
        if (Instant.now().isBefore(unlockAt)) {
            return LockState.locked(unlockAt);
        }
        return LockState.unlocked();
    }

    public void assertUnlocked(String username) throws ValidInputException, NotFoundException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        LockState state = getLockState(user);
        if (state.locked) {
            throw new ValidInputException(
                    "Бюджет заморожен на месяц. Следующее редактирование — " + state.unlockAtHuman(),
                    List.of("budget locked")
            );
        }
    }

    @Getter
    public static final class LockState {
        private final boolean locked;
        private final Instant unlockAt;

        private LockState(boolean locked, Instant unlockAt) {
            this.locked = locked;
            this.unlockAt = unlockAt;
        }

        public static LockState locked(Instant unlockAt) {
            return new LockState(true, unlockAt);
        }

        public static LockState unlocked() {
            return new LockState(false, null);
        }

        public String unlockAtHuman() {
            if (unlockAt == null) return null;
            return unlockAt.atZone(ZONE).format(HUMAN);
        }
    }
}
