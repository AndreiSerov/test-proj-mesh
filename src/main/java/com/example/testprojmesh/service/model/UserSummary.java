package com.example.testprojmesh.service.model;

import java.time.LocalDate;
import java.util.List;

public record UserSummary(
    Long id,
    String name,
    LocalDate dateOfBirth,
    List<ContactView> emails,
    List<ContactView> phones) {
}
