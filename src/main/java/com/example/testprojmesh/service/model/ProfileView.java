package com.example.testprojmesh.service.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** A User's profile; {@code balance} is null, and left out of the JSON, unless the viewer is the owner. */
public record ProfileView(
    Long id,
    String name,
    LocalDate dateOfBirth,
    List<ContactView> emails,
    List<ContactView> phones,
    @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal balance) {
}
