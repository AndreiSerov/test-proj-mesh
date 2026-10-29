package com.example.testprojmesh.api.controller;

import com.example.testprojmesh.api.dto.EmailRequest;
import com.example.testprojmesh.config.OpenApiConfig;
import com.example.testprojmesh.security.TokenService;
import com.example.testprojmesh.service.EmailService;
import com.example.testprojmesh.service.model.ContactView;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {userId} must equal the token's USER_ID; SecurityConfig refuses anything else with 403 (ADR 0003). The services
 * still receive the owner from the token, so the path only addresses the resource.
 */
@RestController
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RequestMapping("/api/users/{userId}/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emails;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContactView add(@AuthenticationPrincipal Jwt jwt, @PathVariable long userId,
                           @Valid @RequestBody EmailRequest request) {
        return emails.add(TokenService.userId(jwt), request.value());
    }

    @PutMapping("/{id}")
    public ContactView change(@AuthenticationPrincipal Jwt jwt, @PathVariable long userId, @PathVariable long id,
                              @Valid @RequestBody EmailRequest request) {
        return emails.change(TokenService.userId(jwt), id, request.value());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable long userId, @PathVariable long id) {
        emails.delete(TokenService.userId(jwt), id);
    }
}
