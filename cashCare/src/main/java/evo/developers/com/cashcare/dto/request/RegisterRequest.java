package evo.developers.com.cashcare.dto.request;

import evo.developers.com.cashcare.model.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "email is required")
    @Email(message = "invalid email")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Username is required")
    private String username;

    @Min(value = 18, message = "must be 18+")
    private int age;

    @NotNull
    private Gender gender;

    @NotBlank(message = "First name is requred")
    private String firstName;

    @NotBlank(message = "First lastname is requred")
    private String lastName;
}
