package com.example.testprojmesh.service.model;

import java.time.LocalDate;

public record UserSearchCriteria(LocalDate dateOfBirthAfter, String phone, String namePrefix, String email) {
}
