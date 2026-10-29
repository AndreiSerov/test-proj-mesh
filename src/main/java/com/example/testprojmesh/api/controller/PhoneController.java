package com.example.testprojmesh.api.controller;

import com.example.testprojmesh.api.dto.PhoneRequest;
import com.example.testprojmesh.config.OpenApiConfig;
import com.example.testprojmesh.security.TokenService;
import com.example.testprojmesh.service.PhoneService;
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

/** Same ownership rule as {@link EmailController}: SecurityConfig checks {userId}, the owner comes from the token. */
@RestController
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RequestMapping("/api/users/{userId}/phones")
@RequiredArgsConstructor
public class PhoneController {

    private final PhoneService phones;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContactView add(@AuthenticationPrincipal Jwt jwt, @PathVariable long userId,
                           @Valid @RequestBody PhoneRequest request) {
        return phones.add(TokenService.userId(jwt), request.value());
    }

    @PutMapping("/{id}")
    public ContactView change(@AuthenticationPrincipal Jwt jwt, @PathVariable long userId, @PathVariable long id,
                              @Valid @RequestBody PhoneRequest request) {
        return phones.change(TokenService.userId(jwt), id, request.value());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable long userId, @PathVariable long id) {
        phones.delete(TokenService.userId(jwt), id);
    }
}
