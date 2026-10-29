package com.example.testprojmesh.dao.repository;

import com.example.testprojmesh.dao.entity.UserEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {

    @Query("select e.user from EmailEntity e where e.value = :value")
    Optional<UserEntity> findByEmail(String value);

    @Query("select p.user from PhoneEntity p where p.value = :value")
    Optional<UserEntity> findByPhone(String value);

    /** Locks the users row (SELECT ... FOR UPDATE) to serialize Contact writes of one User; never touches accounts. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id = :id")
    Optional<UserEntity> lockById(@Param("id") Long id);
}
