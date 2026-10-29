package com.example.testprojmesh.service.model;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferView(Long id, Long fromUserId, Long toUserId, BigDecimal amount, Instant createdAt) {
}
