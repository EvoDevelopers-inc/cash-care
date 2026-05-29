package evo.developers.com.cashcare.service;

import evo.developers.com.cashcare.entity.UserEntity;
import evo.developers.com.cashcare.exception.UnauthorizedException;
import evo.developers.com.cashcare.jpa.UserRepository;
import evo.developers.com.cashcare.model.JwtToken;
import evo.developers.com.cashcare.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public JwtToken login(String login, String password) throws UnauthorizedException {
        UserEntity user = userRepository.findByEmail(login)
                .or(() -> userRepository.findByUsername(login))
                .orElseThrow(() -> new UnauthorizedException("Invalid email/username or password"));

        if (!passwordEncoder.matches(password, user.getHashPassword())) {
            throw new UnauthorizedException("Invalid email/username or password");
        }

        return jwtService.generateTokenPair(user);
    }

    public JwtToken refresh(String refreshToken) throws UnauthorizedException {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        String username = jwtService.extractUsername(refreshToken);
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        UserPrincipal principal = new UserPrincipal(user);
        if (!jwtService.isTokenValid(refreshToken, principal)) {
            throw new UnauthorizedException("Refresh token expired or invalid");
        }

        return jwtService.generateTokenPair(user);
    }

    public UserEntity getCurrentUser(String username) throws UnauthorizedException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }
}
