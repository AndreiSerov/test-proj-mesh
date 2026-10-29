package com.example.testprojmesh.dao;

import com.example.testprojmesh.dao.entity.UserEntity;
import com.example.testprojmesh.dao.repository.UserRepository;
import com.example.testprojmesh.support.BaseIntegrationTest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

class SeedDataTest extends BaseIntegrationTest {

    @Autowired
    UserRepository users;

    @Test
    @Transactional
    void seededUsersEachHaveAnAccountAContactOfEveryKindAndAPasswordHash() {
        List<UserEntity> all = users.findAll();

        assertThat(all).hasSizeGreaterThanOrEqualTo(3);
        assertThat(all).allSatisfy(user -> {
            assertThat(user.getPasswordHash()).startsWith("$2");
            assertThat(user.getEmails()).isNotEmpty();
            assertThat(user.getPhones()).isNotEmpty();
            assertThat(user.getAccount().getInitialDeposit()).isGreaterThan(BigDecimal.ZERO);
            assertThat(user.getAccount().getBalance())
                .isEqualByComparingTo(user.getAccount().getInitialDeposit());
        });
    }
}
