package com.example.testprojmesh.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record TransferRequest(
    @NotNull Long toUserId,
    @NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal amount) {
}
