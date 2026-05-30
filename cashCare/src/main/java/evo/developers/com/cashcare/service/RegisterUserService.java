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

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class RegisterUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FinancesService financesService;

    @Transactional
    public void create(String email, String username, String firstName, String lastName, String password, int age, Gender gender)
            throws BaseException {

        validateInput(username, password);

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setUsername(username);
        user.setGender(gender);
        user.setAge(age);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setHashPassword(passwordEncoder.encode(password));
        user.setInit(false);

        saveUser(user);
        financesService.setupDefaultFinancesForUser(user);
    }

    public void saveUser(UserEntity user) throws ValidInputException {

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            log.error("User with email {} already exists", user.getEmail());
            throw new ValidInputException("Email or username is already taken", List.of("email is taken!", "password is taken!"));
        }
    }

    public void validateInput(String username, String password) throws ValidInputException {
        if (username.length() < 5) {
            throw new ValidInputException("The username is too short, please write at least 6 characters.",
                    List.of("username is too short"));
        }

        if (password.length() < 6) {
            throw new ValidInputException("The password is too short, please write at least 6 characters.",
                    List.of("password is too short"));

        }
    }

}
