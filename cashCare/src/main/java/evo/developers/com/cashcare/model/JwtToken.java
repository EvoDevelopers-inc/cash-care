package evo.developers.com.cashcare.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class JwtToken {
    private final String accessToken;
    private final String refreshToken;
}
