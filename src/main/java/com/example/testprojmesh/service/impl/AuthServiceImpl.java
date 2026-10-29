package com.example.testprojmesh.service.impl;

import com.example.testprojmesh.api.error.ApiException;
import com.example.testprojmesh.dao.entity.UserEntity;
import com.example.testprojmesh.dao.repository.UserRepository;
import com.example.testprojmesh.security.TokenService;
import com.example.testprojmesh.service.AuthService;
import com.example.testprojmesh.util.ContactNormalizer;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid credentials";

    /** BCrypt hash (cost 10, same as real ones) of a random string; checked when no User matches. */
    private static final String DUMMY_HASH = "$2a$10$6hRlxgeC8ITipr3wn6BuOeuROKwgnR6kt8EDPzUI/5wkWJWGkWmjO";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;

    @Override
    @Transactional(readOnly = true)
    public String login(String identifier, String password) {
        Optional<UserEntity> found = find(identifier);
        // Unknown identifiers pay the same BCrypt cost, so response time does not reveal which Contacts exist.
        boolean matches = passwordEncoder.matches(password, found.map(UserEntity::getPasswordHash).orElse(DUMMY_HASH));
        if (found.isEmpty() || !matches) {
            log.warn("Failed login attempt");
            throw ApiException.unauthorized(INVALID_CREDENTIALS);
        }
        UserEntity user = found.get();
        log.info("User {} logged in", user.getId());
        return tokens.issue(user.getId());
    }

    private Optional<UserEntity> find(String identifier) {
        if (identifier.contains("@")) {
            return users.findByEmail(ContactNormalizer.email(identifier));
        }
        String digits = ContactNormalizer.phone(identifier);
        return digits.isEmpty() ? Optional.empty() : users.findByPhone(digits);
    }
}
