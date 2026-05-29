package evo.developers.com.cashcare.dto.response;

import evo.developers.com.cashcare.dto.base.Response;
import evo.developers.com.cashcare.model.JwtToken;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse extends Response {
    private String accessToken;
    private String refreshToken;

    public static AuthResponse from(JwtToken token) {
        AuthResponse response = new AuthResponse();
        response.setStatus(true);
        response.setMessage("Successfully authenticated");
        response.setAccessToken(token.getAccessToken());
        response.setRefreshToken(token.getRefreshToken());
        return response;
    }
}
