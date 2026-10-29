package com.example.testprojmesh.service;

import com.example.testprojmesh.api.error.ApiException;
import com.example.testprojmesh.dao.entity.UserEntity;
import com.example.testprojmesh.support.BaseIntegrationTest;
import com.example.testprojmesh.support.TestUsers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

class TransferConcurrencyTest extends BaseIntegrationTest {

    static final int THREADS = 16;

    @Autowired
    TransferService transferService;

    @Autowired
    TestUsers testUsers;

    @Autowired
    JdbcTemplate jdbc;

    UserEntity userWith(String balance) {
        return testUsers.create("Racer", LocalDate.of(1990, 1, 1), new BigDecimal(balance));
    }

    Map<Long, BigDecimal> balances(List<Long> ids) {
        return jdbc.query("select user_id, balance from accounts where user_id = any (?)",
                ps -> ps.setArray(1, ps.getConnection().createArrayOf("bigint", ids.toArray())),
                (rs, i) -> Map.entry(rs.getLong(1), rs.getBigDecimal(2)))
            .stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /** Runs all tasks at the same instant on a thread pool; fails the test on deadlock or timeout. */
    List<Throwable> runTogether(List<Callable<Void>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(pool.submit(() -> {
                start.await();
                return task.call();
            }));
        }
        start.countDown();
        List<Throwable> failures = new ArrayList<>();
        for (Future<Void> future : futures) {
            try {
                future.get(60, TimeUnit.SECONDS);
            } catch (java.util.concurrent.ExecutionException e) {
                failures.add(e.getCause());
            }
        }
        pool.shutdownNow();
        return failures;
    }

    @Test
    void opposingAndOverlappingTransfersConserveMoneyNeverGoNegativeAndNeverDeadlock() throws Exception {
        List<UserEntity> users = List.of(userWith("1000.00"), userWith("1000.00"),
            userWith("1000.00"), userWith("1000.00"));
        List<Long> ids = users.stream().map(UserEntity::getId).toList();
        Map<Long, BigDecimal> expected = new java.util.concurrent.ConcurrentHashMap<>();
        ids.forEach(id -> expected.put(id, new BigDecimal("1000.00")));
        AtomicInteger succeeded = new AtomicInteger();

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            long from = ids.get(ThreadLocalRandom.current().nextInt(ids.size()));
            long to = ids.get(ThreadLocalRandom.current().nextInt(ids.size()));
            BigDecimal amount = BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(100, 5000), 2);
            tasks.add(() -> {
                try {
                    transferService.transfer(from, to, amount);
                    succeeded.incrementAndGet();
                    expected.merge(from, amount.negate(), BigDecimal::add);
                    expected.merge(to, amount, BigDecimal::add);
                } catch (ApiException e) {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
                }
                return null;
            });
        }

        List<Throwable> failures = runTogether(tasks);

        assertThat(failures).as("unexpected failures (deadlocks, constraint violations)").isEmpty();
        Map<Long, BigDecimal> actual = balances(ids);
        assertThat(actual.values()).allSatisfy(b -> assertThat(b).isGreaterThanOrEqualTo(BigDecimal.ZERO));
        assertThat(actual.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("4000.00");
        ids.forEach(id -> assertThat(actual.get(id)).as("balance of user " + id)
            .isEqualByComparingTo(expected.get(id)));
        String idList = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        Integer recorded = jdbc.queryForObject(
            "select count(*) from transfers where from_user_id in (" + idList + ") and to_user_id in (" + idList + ")",
            Integer.class);
        assertThat(recorded).isEqualTo(succeeded.get());
        assertThat(succeeded.get()).as("most transfers should succeed").isGreaterThan(100);
    }

    @Test
    void oversubscribedSenderNeverGoesNegativeAndExactlyAffordableTransfersSucceed() throws Exception {
        UserEntity sender = userWith("100.00");
        List<UserEntity> recipients = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            recipients.add(userWith("1.00"));
        }
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            long to = recipients.get(i % recipients.size()).getId();
            tasks.add(() -> {
                try {
                    transferService.transfer(sender.getId(), to, new BigDecimal("10.00"));
                    succeeded.incrementAndGet();
                } catch (ApiException e) {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
                    refused.incrementAndGet();
                }
                return null;
            });
        }

        List<Throwable> failures = runTogether(tasks);

        assertThat(failures).isEmpty();
        assertThat(succeeded.get()).isEqualTo(10);
        assertThat(refused.get()).isEqualTo(30);
        List<Long> all = new ArrayList<>(recipients.stream().map(UserEntity::getId).toList());
        all.add(sender.getId());
        Map<Long, BigDecimal> actual = balances(all);
        assertThat(actual.get(sender.getId())).isEqualByComparingTo("0.00");
        assertThat(actual.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("105.00");
    }
}
