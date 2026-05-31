package evo.developers.com.cashcare.service;

import evo.developers.com.cashcare.dto.request.UserSurveyRequest;
import evo.developers.com.cashcare.dto.response.UserSurveyResponse;
import evo.developers.com.cashcare.entity.UserEntity;
import evo.developers.com.cashcare.exception.NotFoundException;
import evo.developers.com.cashcare.jpa.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserEntity getUserByUsername(String username) throws NotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public UserSurveyResponse getSurvey(String username) throws NotFoundException {
        return UserSurveyResponse.from(getUserByUsername(username));
    }

    @Transactional
    public UserSurveyResponse saveSurvey(String username, UserSurveyRequest request) throws NotFoundException {
        UserEntity user = getUserByUsername(username);

        user.setMaritalStatus(request.getMaritalStatus());
        user.setChildrenCount(request.getChildrenCount());
        user.setEmploymentType(request.getEmploymentType());
        user.setHousingStatus(request.getHousingStatus());
        user.setHasDebts(request.getHasDebts());
        user.setFinancialGoal(request.getFinancialGoal());
        user.setCitySize(request.getCitySize());
        user.setSpendingStyle(request.getSpendingStyle());
        user.setSurveyCompleted(true);

        userRepository.save(user);
        return UserSurveyResponse.from(user);
    }
}
