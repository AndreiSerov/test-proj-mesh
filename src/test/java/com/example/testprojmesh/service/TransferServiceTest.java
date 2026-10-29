package com.example.testprojmesh.service;

import com.example.testprojmesh.api.error.ApiException;
import com.example.testprojmesh.dao.entity.AccountEntity;
import com.example.testprojmesh.dao.entity.TransferEntity;
import com.example.testprojmesh.dao.entity.UserEntity;
import com.example.testprojmesh.dao.repository.AccountRepository;
import com.example.testprojmesh.dao.repository.TransferRepository;
import com.example.testprojmesh.service.impl.TransferServiceImpl;
import com.example.testprojmesh.service.model.TransferView;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransferServiceTest {

    AccountRepository accounts = mock(AccountRepository.class);
    TransferRepository transfers = mock(TransferRepository.class);
    TransferService service = new TransferServiceImpl(accounts, transfers);

    AccountEntity sender;
    AccountEntity recipient;

    @BeforeEach
    void setUp() {
        sender = account(1L, 10L, "100.00");
        recipient = account(2L, 20L, "5.00");
        when(accounts.lockByUserIds(any())).thenReturn(List.of(sender, recipient));
        when(transfers.save(any(TransferEntity.class))).thenAnswer(inv -> {
            TransferEntity t = inv.getArgument(0);
            t.setId(77L);
            return t;
        });
    }

    static AccountEntity account(long accountId, long userId, String balance) {
        UserEntity user = new UserEntity();
        user.setId(userId);
        AccountEntity account = new AccountEntity();
        account.setId(accountId);
        account.setUser(user);
        account.setBalance(new BigDecimal(balance));
        account.setInitialDeposit(new BigDecimal(balance));
        return account;
    }

    static void assertRejected(Runnable action, HttpStatus status) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(status));
    }

    @Test
    void movesExactlyTheAmountFromSenderToRecipientAndRecordsTheTransfer() {
        TransferView view = service.transfer(10L, 20L, new BigDecimal("25.50"));

        assertThat(sender.getBalance()).isEqualByComparingTo("74.50");
        assertThat(recipient.getBalance()).isEqualByComparingTo("30.50");
        ArgumentCaptor<TransferEntity> saved = ArgumentCaptor.forClass(TransferEntity.class);
        verify(transfers).save(saved.capture());
        assertThat(saved.getValue().getFromUserId()).isEqualTo(10L);
        assertThat(saved.getValue().getToUserId()).isEqualTo(20L);
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("25.50");
        assertThat(saved.getValue().getCreatedAt()).isNotNull();
        assertThat(view.id()).isEqualTo(77L);
        assertThat(view.amount()).isEqualByComparingTo("25.50");
    }

    @Test
    void sendingTheWholeBalanceLeavesZero() {
        service.transfer(10L, 20L, new BigDecimal("100.00"));

        assertThat(sender.getBalance()).isEqualByComparingTo("0.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("105.00");
    }

    @Test
    void wholeNumberAmountsAreAccepted() {
        service.transfer(10L, 20L, new BigDecimal("3"));

        assertThat(sender.getBalance()).isEqualByComparingTo("97.00");
    }

    @Test
    void zeroNegativeAndOverPreciseAmountsAreRejectedBeforeAnythingIsLocked() {
        for (String bad : List.of("0", "0.00", "-1.00", "10.001")) {
            assertRejected(() -> service.transfer(10L, 20L, new BigDecimal(bad)), HttpStatus.BAD_REQUEST);
        }
        assertRejected(() -> service.transfer(10L, 20L, null), HttpStatus.BAD_REQUEST);
        verifyNoInteractions(accounts, transfers);
    }

    @Test
    void transferToYourselfIsRejectedBeforeAnythingIsLocked() {
        assertRejected(() -> service.transfer(10L, 10L, new BigDecimal("1.00")), HttpStatus.UNPROCESSABLE_CONTENT);
        verifyNoInteractions(accounts, transfers);
    }

    @Test
    void unknownRecipientIsNotFoundAndNothingChanges() {
        when(accounts.lockByUserIds(any())).thenReturn(List.of(sender));

        assertRejected(() -> service.transfer(10L, 999L, new BigDecimal("1.00")), HttpStatus.NOT_FOUND);

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        verify(transfers, never()).save(any());
    }

    @Test
    void insufficientBalanceIsRejectedAndNothingChanges() {
        assertRejected(() -> service.transfer(10L, 20L, new BigDecimal("100.01")), HttpStatus.UNPROCESSABLE_CONTENT);

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("5.00");
        verify(transfers, never()).save(any());
    }

    @Test
    void bothAccountsAreLockedTogetherSoTheDatabaseCanLockThemInIdOrder() {
        service.transfer(10L, 20L, new BigDecimal("1.00"));

        verify(accounts).lockByUserIds(List.of(10L, 20L));
    }

    @Test
    void transferThatWouldOverflowTheRecipientBalanceIsRejectedAndNothingChanges() {
        recipient.setBalance(new BigDecimal("99999999999999999.50"));

        assertRejected(() -> service.transfer(10L, 20L, new BigDecimal("1.00")), HttpStatus.UNPROCESSABLE_CONTENT);

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("99999999999999999.50");
        verify(transfers, never()).save(any());
    }

    @Test
    void transferThatFillsTheRecipientBalanceExactlyToTheLimitSucceeds() {
        recipient.setBalance(new BigDecimal("99999999999999999.50"));

        service.transfer(10L, 20L, new BigDecimal("0.49"));

        assertThat(recipient.getBalance()).isEqualByComparingTo("99999999999999999.99");
    }
}
