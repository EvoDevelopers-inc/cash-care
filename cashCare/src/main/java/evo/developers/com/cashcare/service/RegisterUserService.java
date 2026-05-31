package evo.developers.com.cashcare.service;

import evo.developers.com.cashcare.entity.UserEntity;
import evo.developers.com.cashcare.exception.BaseException;
import evo.developers.com.cashcare.exception.ValidInputException;
import evo.developers.com.cashcare.jpa.UserRepository;
import evo.developers.com.cashcare.model.Gender;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class RegisterUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FinancesService financesService;

    @Transactional(rollbackFor = Exception.class)
    public void create(String email, String username, String firstName, String lastName, String password, int age, Gender gender)
            throws BaseException {

        validateInput(email, username, password);
        validateUniqueness(email, username);

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setUsername(username);
        user.setGender(gender);
        user.setAge(age);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setHashPassword(passwordEncoder.encode(password));
        user.setInit(false);

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            log.warn("Race condition on register: {} / {}", email, username);
            throw new ValidInputException(
                    "Такой email или логин уже зарегистрирован",
                    List.of("email or username is taken")
            );
        }

        financesService.setupDefaultFinancesForUser(user);
    }

    private void validateUniqueness(String email, String username) throws ValidInputException {
        List<String> details = new ArrayList<>();
        if (userRepository.existsByEmail(email)) {
            details.add("email уже зарегистрирован");
        }
        if (userRepository.existsByUsername(username)) {
            details.add("логин уже занят");
        }
        if (!details.isEmpty()) {
            String head = details.size() == 2
                    ? "Email и логин уже заняты"
                    : (details.get(0).startsWith("email") ? "Email уже зарегистрирован" : "Логин уже занят");
            throw new ValidInputException(head, details);
        }
    }

    private void validateInput(String email, String username, String password) throws ValidInputException {
        if (email == null || email.isBlank()) {
            throw new ValidInputException("Укажи email", List.of("email is empty"));
        }
        if (username == null || username.length() < 5) {
            throw new ValidInputException(
                    "Логин слишком короткий — минимум 5 символов",
                    List.of("username is too short")
            );
        }
        if (password == null || password.length() < 6) {
            throw new ValidInputException(
                    "Пароль слишком короткий — минимум 6 символов",
                    List.of("password is too short")
            );
        }
    }
}
