package evo.developers.com.cashcare.service;

import evo.developers.com.cashcare.entity.UserEntity;
import evo.developers.com.cashcare.exception.BaseException;
import evo.developers.com.cashcare.exception.NotFoundException;
import evo.developers.com.cashcare.exception.ValidInputException;
import evo.developers.com.cashcare.jpa.UserRepository;
import evo.developers.com.cashcare.model.Gender;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserEntity getUserByUsername(String username) throws NotFoundException {
        Optional<UserEntity> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            throw new NotFoundException("User not found");
        }

        return null;
    }
}
