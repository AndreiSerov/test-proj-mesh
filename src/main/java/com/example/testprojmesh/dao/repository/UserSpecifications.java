package com.example.testprojmesh.dao.repository;

import com.example.testprojmesh.dao.entity.UserEntity;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<UserEntity> bornAfter(LocalDate date) {
        return (root, query, cb) -> cb.greaterThan(root.get("dateOfBirth"), date);
    }

    public static Specification<UserEntity> nameStartsWith(String prefix) {
        String pattern = escapeLike(prefix.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\');
    }

    public static Specification<UserEntity> hasEmail(String email) {
        return (root, query, cb) -> cb.equal(root.join("emails").get("value"), email);
    }

    public static Specification<UserEntity> hasPhone(String phone) {
        return (root, query, cb) -> cb.equal(root.join("phones").get("value"), phone);
    }

    private static String escapeLike(String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
