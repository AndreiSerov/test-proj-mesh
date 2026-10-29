package com.example.testprojmesh.interest;

import com.example.testprojmesh.dao.entity.UserEntity;
import com.example.testprojmesh.support.BaseIntegrationTest;
import com.example.testprojmesh.support.TestUsers;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
    "test.context=interest-scheduler",
    "app.interest.enabled=true",
    "app.interest.interval=PT0.3S"
})
class InterestSchedulerTest extends BaseIntegrationTest {

    @Autowired
    TestUsers testUsers;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void theScheduledJobAppliesInterestOnItsOwn() {
        UserEntity saver = testUsers.create("Saver", LocalDate.of(1990, 1, 1), new BigDecimal("100.00"));

        Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            BigDecimal balance = jdbc.queryForObject(
                "select balance from accounts where user_id = ?", BigDecimal.class, saver.getId());
            assertThat(balance).isGreaterThan(new BigDecimal("100.00"));
        });
    }
}
