package evo.developers.com.cashcare.dto.response;

import evo.developers.com.cashcare.entity.UserEntity;
import evo.developers.com.cashcare.model.Gender;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileResponse {
    private int id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private int age;
    private Gender gender;

    public static UserProfileResponse from(UserEntity user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setAge(user.getAge());
        response.setGender(user.getGender());
        return response;
    }
}
