package com.example.testprojmesh.api.dto;

import jakarta.validation.constraints.NotBlank;

public record PhoneRequest(@NotBlank @ValidPhone String value) {
}
