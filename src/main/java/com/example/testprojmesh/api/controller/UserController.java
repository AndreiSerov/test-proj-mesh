package com.example.testprojmesh.api.controller;

import com.example.testprojmesh.api.dto.ValidPhone;
import com.example.testprojmesh.config.OpenApiConfig;
import com.example.testprojmesh.security.TokenService;
import com.example.testprojmesh.service.UserService;
import com.example.testprojmesh.service.model.PageView;
import com.example.testprojmesh.service.model.ProfileView;
import com.example.testprojmesh.service.model.UserSearchCriteria;
import com.example.testprojmesh.service.model.UserSummary;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService users;

    @GetMapping("/{userId}")
    public ProfileView profile(@AuthenticationPrincipal Jwt jwt, @PathVariable long userId) {
        return users.profile(TokenService.userId(jwt), userId);
    }

    @GetMapping("/search")
    public PageView<UserSummary> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
            @RequestParam(required = false) @ValidPhone String phone,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return users.search(new UserSearchCriteria(dateOfBirth, phone, name, email), page, size);
    }
}
