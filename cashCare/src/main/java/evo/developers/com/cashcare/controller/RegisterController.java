package evo.developers.com.cashcare.controller;

import evo.developers.com.cashcare.dto.base.Response;
import evo.developers.com.cashcare.dto.request.RegisterRequest;
import evo.developers.com.cashcare.dto.response.RegisterResponse;
import evo.developers.com.cashcare.exception.BaseException;
import evo.developers.com.cashcare.service.RegisterUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class RegisterController {

    private final RegisterUserService registerService;

    @PostMapping("/register")
    @SecurityRequirements
    public ResponseEntity<? extends Response> registerUser(@Valid @RequestBody RegisterRequest body) throws BaseException {
        registerService.create(body.getEmail(), body.getUsername(), body.getFirstName(), body.getLastName(), body.getPassword(), body.getAge(), body.getGender());

        Response r = new RegisterResponse();
        r.setMessage("Successfully registered!");
        r.setStatus(true);

        return ResponseEntity.ok(r);
    }
}
