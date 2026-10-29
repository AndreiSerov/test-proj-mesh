package com.example.testprojmesh.service.impl;

import com.example.testprojmesh.api.error.ApiException;
import com.example.testprojmesh.dao.entity.AccountEntity;
import com.example.testprojmesh.dao.entity.TransferEntity;
import com.example.testprojmesh.dao.repository.AccountRepository;
import com.example.testprojmesh.dao.repository.TransferRepository;
import com.example.testprojmesh.service.TransferService;
import com.example.testprojmesh.service.model.TransferView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private static final int MONEY_SCALE = 2;

    /** Largest value of the NUMERIC(19,2) Balance column. */
    private static final BigDecimal MAX_BALANCE = new BigDecimal("99999999999999999.99");

    private final AccountRepository accounts;
    private final TransferRepository transfers;

    @Override
    @Transactional
    public TransferView transfer(long fromUserId, long toUserId, BigDecimal rawAmount) {
        BigDecimal amount = validAmount(rawAmount);
        if (fromUserId == toUserId) {
            throw reject("Transfer from user {} to themselves", fromUserId,
                ApiException.unprocessable("Cannot transfer to yourself"));
        }

        List<AccountEntity> locked = accounts.lockByUserIds(List.of(fromUserId, toUserId));
        AccountEntity sender = accountOf(locked, fromUserId);
        AccountEntity recipient = accountOf(locked, toUserId);
        if (sender == null) {
            throw reject("Sender {} has no account", fromUserId, ApiException.notFound("Sender not found"));
        }
        if (recipient == null) {
            throw reject("Transfer from user {} to unknown user {}", fromUserId, toUserId,
                ApiException.notFound("Recipient not found: " + toUserId));
        }
        if (sender.getBalance().compareTo(amount) < 0) {
            throw reject("Transfer from user {} to user {} refused: insufficient funds", fromUserId, toUserId,
                ApiException.unprocessable("Insufficient funds"));
        }
        if (recipient.getBalance().add(amount).compareTo(MAX_BALANCE) > 0) {
            throw reject("Transfer from user {} to user {} refused: recipient balance would overflow", fromUserId,
                toUserId, ApiException.unprocessable("Recipient balance would exceed the maximum"));
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        recipient.setBalance(recipient.getBalance().add(amount));

        TransferEntity record = new TransferEntity();
        record.setFromUserId(fromUserId);
        record.setToUserId(toUserId);
        record.setAmount(amount);
        record.setCreatedAt(Instant.now());
        TransferEntity saved = transfers.save(record);

        log.info("Transfer {} completed: user {} -> user {}", saved.getId(), fromUserId, toUserId);
        return new TransferView(saved.getId(), fromUserId, toUserId, saved.getAmount(), saved.getCreatedAt());
    }

    private static BigDecimal validAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw ApiException.invalid("Amount must be greater than zero");
        }
        if (amount.stripTrailingZeros().scale() > MONEY_SCALE) {
            throw ApiException.invalid("Amount must have at most 2 decimal places");
        }
        return amount.setScale(MONEY_SCALE);
    }

    private static AccountEntity accountOf(List<AccountEntity> locked, long userId) {
        return locked.stream()
            .filter(a -> a.getUser().getId() == userId)
            .findFirst()
            .orElse(null);
    }

    private static ApiException reject(String message, Object arg, ApiException exception) {
        log.warn(message, arg);
        return exception;
    }

    private static ApiException reject(String message, Object first, Object second, ApiException exception) {
        log.warn(message, first, second);
        return exception;
    }
}
