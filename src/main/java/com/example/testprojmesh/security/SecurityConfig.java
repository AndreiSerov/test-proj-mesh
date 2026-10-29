package com.example.testprojmesh.security;

import com.example.testprojmesh.api.error.ProblemResponses;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JsonMapper json) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/actuator/health/**",
                    "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/users/{userId}/emails/**", "/api/users/{userId}/phones/**")
                    .access(SecurityConfig::ownerOnly)
                .anyRequest().authenticated())
            .oauth2ResourceServer(rs -> rs
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint(unauthorizedEntryPoint(json)))
            .exceptionHandling(e -> e
                .authenticationEntryPoint(unauthorizedEntryPoint(json))
                .accessDeniedHandler(forbiddenHandler(json)))
            .build();
    }

    /**
     * Grants Contact writes only when the {userId} path segment is the token's USER_ID (ADR 0003). Runs in the
     * filter chain, so another User's id is refused before body validation or any service code.
     */
    private static AuthorizationDecision ownerOnly(Supplier<? extends Authentication> authentication,
                                                   RequestAuthorizationContext context) {
        if (!(authentication.get() instanceof JwtAuthenticationToken token)) {
            return new AuthorizationDecision(false);
        }
        try {
            long pathUserId = Long.parseLong(context.getVariables().get("userId"));
            return new AuthorizationDecision(pathUserId == TokenService.userId(token.getToken()));
        } catch (NumberFormatException e) {
            return new AuthorizationDecision(false);
        }
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key(properties)));
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key(properties))
            .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefault(), SecurityConfig::requireNumericUserId));
        return decoder;
    }

    /** Rejects a correctly signed token without a numeric USER_ID, so it fails with 401 instead of a 500 later. */
    private static OAuth2TokenValidatorResult requireNumericUserId(Jwt jwt) {
        if (jwt.getClaim(TokenService.USER_ID_CLAIM) instanceof Number) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(
            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "USER_ID claim is missing or not numeric", null));
    }

    private static SecretKey key(JwtProperties properties) {
        return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static AuthenticationEntryPoint unauthorizedEntryPoint(JsonMapper json) {
        return (request, response, ex) ->
            ProblemResponses.write(request, response, json, HttpStatus.UNAUTHORIZED, "Authentication is required");
    }

    private static AccessDeniedHandler forbiddenHandler(JsonMapper json) {
        return (request, response, ex) -> ProblemResponses.write(request, response, json, HttpStatus.FORBIDDEN,
            "Only the owner can change these Contacts");
    }
}
