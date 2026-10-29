package com.example.testprojmesh.dao.repository;

import com.example.testprojmesh.dao.entity.ContactEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface ContactRepository<E extends ContactEntity> extends JpaRepository<E, Long> {

    boolean existsByValue(String value);

    boolean existsByValueAndUserId(String value, Long userId);

    boolean existsByValueAndIdNot(String value, Long id);

    Optional<E> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
