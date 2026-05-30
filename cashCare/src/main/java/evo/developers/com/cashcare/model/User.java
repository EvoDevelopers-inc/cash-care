package evo.developers.com.cashcare.model;

import evo.developers.com.cashcare.entity.UserEntity;
import lombok.Getter;

@Getter
public class User {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private int age;
    private Gender gender;

    public static User from(UserEntity userEntity) {

        User instance = new User();
        instance.username = userEntity.getFirstName();
        instance.firstName = userEntity.getFirstName();
        instance.lastName = userEntity.getLastName();
        instance.age = userEntity.getAge();
        instance.gender = userEntity.getGender();
        instance.email = userEntity.getEmail();

        return instance;

    }
}
