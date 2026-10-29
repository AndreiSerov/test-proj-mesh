package com.example.testprojmesh.security;

import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public class TokenService {

    public static final String USER_ID_CLAIM = "USER_ID";

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public TokenService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    /** The token carries only USER_ID, plus the exp that enforces the one-hour lifetime. */
    public String issue(long userId) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .claim(USER_ID_CLAIM, userId)
            .expiresAt(Instant.now().plus(properties.ttl()))
            .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    }

    public static long userId(Jwt jwt) {
        return ((Number) jwt.getClaim(USER_ID_CLAIM)).longValue();
    }
}
