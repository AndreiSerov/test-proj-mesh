package com.example.testprojmesh.service;

import com.example.testprojmesh.api.error.ApiException;
import com.example.testprojmesh.dao.entity.UserEntity;
import com.example.testprojmesh.support.BaseIntegrationTest;
import com.example.testprojmesh.support.TestUsers;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

class ContactConcurrencyTest extends BaseIntegrationTest {

    static final int ROUNDS = 10;

    @Autowired
    EmailService emailService;

    @Autowired
    PhoneService phoneService;

    @Autowired
    TestUsers testUsers;

    @Autowired
    JdbcTemplate jdbc;

    /** Runs both tasks at the same instant and returns the outcome of each: null for success, else the error. */
    List<Throwable> runTogether(Runnable first, Runnable second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();
        for (Runnable task : List.of(first, second)) {
            Callable<Void> call = () -> {
                start.await();
                task.run();
                return null;
            };
            futures.add(pool.submit(call));
        }
        start.countDown();
        List<Throwable> outcomes = new ArrayList<>();
        for (Future<Void> future : futures) {
            try {
                future.get(60, TimeUnit.SECONDS);
                outcomes.add(null);
            } catch (ExecutionException e) {
                outcomes.add(e.getCause());
            }
        }
        pool.shutdownNow();
        return outcomes;
    }

    void assertExactlyOneRefused(List<Throwable> outcomes) {
        assertThat(outcomes.stream().filter(o -> o == null)).hasSize(1);
        Throwable refused = outcomes.stream().filter(o -> o != null).findFirst().orElseThrow();
        assertThat(refused).isInstanceOf(ApiException.class);
        assertThat(((ApiException) refused).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    long count(String table, long userId) {
        return jdbc.queryForObject("select count(*) from " + table + " where user_id = ?", Long.class, userId);
    }

    @Test
    void parallelDeletesOfTwoEmailsLeaveExactlyOne() throws Exception {
        for (int i = 0; i < ROUNDS; i++) {
            UserEntity user = testUsers.create("Racer");
            long id = user.getId();
            long first = user.getEmails().get(0).getId();
            long second = emailService.add(id, UUID.randomUUID() + "@race.example").id();

            List<Throwable> outcomes = runTogether(
                () -> emailService.delete(id, first), () -> emailService.delete(id, second));

            assertExactlyOneRefused(outcomes);
            assertThat(count("emails", id)).isEqualTo(1);
        }
    }

    @Test
    void parallelDeletesOfTwoPhonesLeaveExactlyOne() throws Exception {
        for (int i = 0; i < ROUNDS; i++) {
            UserEntity user = testUsers.create("Racer");
            long id = user.getId();
            long first = user.getPhones().get(0).getId();
            String number = String.valueOf(80_000_000_000L + Math.abs(UUID.randomUUID().getMostSignificantBits()
                % 1_000_000_000L));
            long second = phoneService.add(id, number).id();

            List<Throwable> outcomes = runTogether(
                () -> phoneService.delete(id, first), () -> phoneService.delete(id, second));

            assertExactlyOneRefused(outcomes);
            assertThat(count("phones", id)).isEqualTo(1);
        }
    }

    @Test
    void twoUsersAddingTheSameEmailAtOnceGetOneCreatedAndOneConflict() throws Exception {
        for (int i = 0; i < ROUNDS; i++) {
            long first = testUsers.create("Racer").getId();
            long second = testUsers.create("Racer").getId();
            String shared = UUID.randomUUID() + "@race.example";

            List<Throwable> outcomes = runTogether(
                () -> emailService.add(first, shared), () -> emailService.add(second, shared));

            assertThat(outcomes.stream().filter(o -> o == null)).hasSize(1);
            Throwable refused = outcomes.stream().filter(o -> o != null).findFirst().orElseThrow();
            assertThat(refused).isInstanceOf(ApiException.class);
            assertThat(((ApiException) refused).getStatus()).isEqualTo(HttpStatus.CONFLICT);
        }
    }
}
