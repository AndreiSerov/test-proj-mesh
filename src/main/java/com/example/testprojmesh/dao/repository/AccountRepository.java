package com.example.testprojmesh.dao.repository;

import com.example.testprojmesh.dao.entity.AccountEntity;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    /** Locks the matching accounts with SELECT ... FOR UPDATE, always in ascending account id order. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountEntity a where a.user.id in :userIds order by a.id")
    List<AccountEntity> lockByUserIds(@Param("userIds") Collection<Long> userIds);

    /**
     * Grows every balance that would actually change in one statement. Rows are locked in ascending id order, the
     * same order transfers use, so the two cannot deadlock. A balance at or above the cap, or too small to grow
     * after rounding (0.04 * 1.10 rounds back to 0.04), is never locked, touched or counted.
     */
    @Modifying(clearAutomatically = true)
    @Query(nativeQuery = true, value = """
        UPDATE accounts a
        SET balance = LEAST(round_half_even(a.balance * :growth), round_half_even(a.initial_deposit * :capFactor))
        FROM (SELECT id FROM accounts
              WHERE LEAST(round_half_even(balance * :growth), round_half_even(initial_deposit * :capFactor))
                    > balance
              ORDER BY id
              FOR UPDATE) locked
        WHERE a.id = locked.id
          AND LEAST(round_half_even(a.balance * :growth), round_half_even(a.initial_deposit * :capFactor))
              > a.balance
        """)
    int accrueInterest(@Param("growth") BigDecimal growth, @Param("capFactor") BigDecimal capFactor);
}
