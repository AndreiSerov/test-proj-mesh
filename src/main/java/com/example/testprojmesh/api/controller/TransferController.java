package com.example.testprojmesh.api.controller;

import com.example.testprojmesh.api.dto.TransferRequest;
import com.example.testprojmesh.config.OpenApiConfig;
import com.example.testprojmesh.security.TokenService;
import com.example.testprojmesh.service.TransferService;
import com.example.testprojmesh.service.model.TransferView;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transfers;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferView transfer(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TransferRequest request) {
        return transfers.transfer(TokenService.userId(jwt), request.toUserId(), request.amount());
    }
}
