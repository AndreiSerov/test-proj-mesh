package com.example.testprojmesh.service;

import com.example.testprojmesh.api.error.ApiException;
import com.example.testprojmesh.dao.repository.UserRepository;
import com.example.testprojmesh.security.TokenService;
import com.example.testprojmesh.service.impl.AuthServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    UserRepository users = mock(UserRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    TokenService tokens = mock(TokenService.class);
    AuthService service = new AuthServiceImpl(users, encoder, tokens);

    @Test
    void unknownEmailStillRunsTheEncoderSoTheTimingMatchesAWrongPassword() {
        when(users.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("nobody@example.com", "secret"))
            .isInstanceOfSatisfying(ApiException.class, e -> org.assertj.core.api.Assertions
                .assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(encoder).matches(eq("secret"), anyString());
        verifyNoInteractions(tokens);
    }

    @Test
    void unknownPhoneStillRunsTheEncoder() {
        when(users.findByPhone("79990000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("+7 999 000 00 00", "secret")).isInstanceOf(ApiException.class);

        verify(encoder).matches(eq("secret"), anyString());
    }

    @Test
    void identifierWithoutDigitsStillRunsTheEncoder() {
        assertThatThrownBy(() -> service.login("---", "secret")).isInstanceOf(ApiException.class);

        verify(encoder).matches(eq("secret"), anyString());
    }
}
