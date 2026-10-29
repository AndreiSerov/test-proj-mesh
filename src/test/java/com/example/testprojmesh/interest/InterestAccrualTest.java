package com.example.testprojmesh.interest;

import com.example.testprojmesh.api.error.ApiException;
import com.example.testprojmesh.dao.entity.UserEntity;
import com.example.testprojmesh.service.InterestService;
import com.example.testprojmesh.service.TransferService;
import com.example.testprojmesh.support.BaseIntegrationTest;
import com.example.testprojmesh.support.TestUsers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

/** Own context (and database): accrual touches every account, so it must not share one with other tests. */
@TestPropertySource(properties = "test.context=interest-accrual")
class InterestAccrualTest extends BaseIntegrationTest {

    @Autowired
    InterestService interest;

    @Autowired
    TransferService transfers;

    @Autowired
    TestUsers testUsers;

    @Autowired
    JdbcTemplate jdbc;

    UserEntity user(String balance, String initialDeposit) {
        UserEntity user = testUsers.create("Saver", LocalDate.of(1990, 1, 1), new BigDecimal(balance));
        jdbc.update("update accounts set initial_deposit = ? where user_id = ?",
            new BigDecimal(initialDeposit), user.getId());
        return user;
    }

    BigDecimal balanceOf(UserEntity user) {
        return jdbc.queryForObject("select balance from accounts where user_id = ?", BigDecimal.class, user.getId());
    }

    @Test
    void aBalanceTooSmallToGrowAfterRoundingIsNotCountedAsChanged() {
        // Every other account above its cap, so only the two below can be counted.
        jdbc.update("update accounts set balance = initial_deposit * 3");
        UserEntity tiny = user("0.04", "100.00");
        UserEntity saver = user("100.00", "100.00");

        int changed = interest.accrue();

        assertThat(changed).isEqualTo(1);
        assertThat(balanceOf(tiny)).isEqualByComparingTo("0.04");
        assertThat(balanceOf(saver)).isEqualByComparingTo("110.00");
    }

    @Test
    void balanceGrowsByTenPercentOfItsCurrentValueEachAccrual() {
        UserEntity saver = user("100.00", "100.00");

        interest.accrue();
        assertThat(balanceOf(saver)).isEqualByComparingTo("110.00");

        interest.accrue();
        assertThat(balanceOf(saver)).isEqualByComparingTo("121.00");
    }

    @Test
    void growthStopsExactlyAtTwoHundredSevenPercentOfTheInitialDeposit() {
        UserEntity saver = user("100.00", "100.00");

        for (int i = 0; i < 12; i++) {
            interest.accrue();
            assertThat(balanceOf(saver)).isLessThanOrEqualTo(new BigDecimal("207.00"));
        }

        assertThat(balanceOf(saver)).isEqualByComparingTo("207.00");
        interest.accrue();
        assertThat(balanceOf(saver)).isEqualByComparingTo("207.00");
    }

    @Test
    void balanceAlreadyAboveTheCapIsLeftUnchanged() {
        UserEntity rich = user("500.00", "100.00");

        interest.accrue();

        assertThat(balanceOf(rich)).isEqualByComparingTo("500.00");
    }

    @Test
    void balanceBelowTheCapResumesEarningAfterMoneyIsSentAway() {
        UserEntity saver = user("207.00", "100.00");
        UserEntity friend = user("1.00", "1.00");
        interest.accrue();
        assertThat(balanceOf(saver)).isEqualByComparingTo("207.00");

        transfers.transfer(saver.getId(), friend.getId(), new BigDecimal("100.00"));
        interest.accrue();

        assertThat(balanceOf(saver)).isEqualByComparingTo("117.70");
    }

    @Test
    void roundingIsHalfEvenToTwoDecimals() {
        UserEntity tieDown = user("0.15", "100.00");
        UserEntity tieUp = user("0.05", "100.00");

        interest.accrue();

        assertThat(balanceOf(tieDown)).isEqualByComparingTo("0.16");
        assertThat(balanceOf(tieUp)).isEqualByComparingTo("0.06");
    }

    @Test
    void accrualReportsHowManyAccountsGrew() {
        UserEntity growing = user("10.00", "10.00");
        UserEntity capped = user("20.70", "10.00");

        int updated = interest.accrue();

        assertThat(updated).isGreaterThanOrEqualTo(1);
        assertThat(balanceOf(growing)).isEqualByComparingTo("11.00");
        assertThat(balanceOf(capped)).isEqualByComparingTo("20.70");
        assertThat(interest.accrue()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void accrualRunningAlongsideTransfersNeitherDeadlocksNorBreaksBalances() throws Exception {
        List<UserEntity> users = List.of(user("1000.00", "1000.00"), user("1000.00", "1000.00"),
            user("1000.00", "1000.00"), user("1000.00", "1000.00"));
        List<Long> ids = users.stream().map(UserEntity::getId).toList();

        ExecutorService pool = Executors.newFixedThreadPool(12);
        List<Future<Void>> futures = new ArrayList<>();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            long from = ids.get(ThreadLocalRandom.current().nextInt(ids.size()));
            long to = ids.get(ThreadLocalRandom.current().nextInt(ids.size()));
            BigDecimal amount = BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(100, 5000), 2);
            tasks.add(() -> {
                try {
                    transfers.transfer(from, to, amount);
                } catch (ApiException e) {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
                }
                return null;
            });
        }
        for (int i = 0; i < 30; i++) {
            tasks.add(() -> {
                interest.accrue();
                return null;
            });
        }
        java.util.Collections.shuffle(tasks);
        tasks.forEach(task -> futures.add(pool.submit(task)));

        for (Future<Void> future : futures) {
            future.get(60, TimeUnit.SECONDS);
        }
        pool.shutdownNow();

        users.forEach(u -> assertThat(balanceOf(u)).isGreaterThanOrEqualTo(BigDecimal.ZERO));
    }
}
