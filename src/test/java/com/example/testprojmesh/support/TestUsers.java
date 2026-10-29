package com.example.testprojmesh.support;

import com.example.testprojmesh.dao.entity.AccountEntity;
import com.example.testprojmesh.dao.entity.EmailEntity;
import com.example.testprojmesh.dao.entity.PhoneEntity;
import com.example.testprojmesh.dao.entity.UserEntity;
import com.example.testprojmesh.dao.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class TestUsers {

    public static final String PASSWORD = "password123";

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public TestUsers(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public UserEntity create(String name, LocalDate dateOfBirth, BigDecimal balance) {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String phone = String.valueOf(Math.abs(suffix.hashCode() % 1_000_000_000L) + 70_000_000_000L);
        return create(name, dateOfBirth, balance, suffix + "@test.example", phone);
    }

    public UserEntity create(String name, LocalDate dateOfBirth, BigDecimal balance, String email, String phone) {
        UserEntity user = new UserEntity();
        user.setName(name);
        user.setDateOfBirth(dateOfBirth);
        user.setPasswordHash(encoder.encode(PASSWORD));

        AccountEntity account = new AccountEntity();
        account.setUser(user);
        account.setBalance(balance);
        account.setInitialDeposit(balance.signum() > 0 ? balance : BigDecimal.ONE);
        user.setAccount(account);

        EmailEntity emailEntity = new EmailEntity();
        emailEntity.setUser(user);
        emailEntity.setValue(email);
        user.getEmails().add(emailEntity);

        PhoneEntity phoneEntity = new PhoneEntity();
        phoneEntity.setUser(user);
        phoneEntity.setValue(phone);
        user.getPhones().add(phoneEntity);

        return users.saveAndFlush(user);
    }

    public UserEntity create(String name) {
        return create(name, LocalDate.of(1992, 1, 1), new BigDecimal("100.00"));
    }
}
