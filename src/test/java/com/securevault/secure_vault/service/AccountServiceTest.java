package com.securevault.secure_vault.service;

import com.securevault.secure_vault.exception.AccountNotFoundException;
import com.securevault.secure_vault.exception.InsufficientFundsException;
import com.securevault.secure_vault.model.Account;
import com.securevault.secure_vault.model.Transaction;
import com.securevault.secure_vault.model.TransactionType;
import com.securevault.secure_vault.model.User;
import com.securevault.secure_vault.repository.AccountRepository;
import com.securevault.secure_vault.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

   @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private Account account;

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("jdoe");

        account = new Account();
        account.setId(1L);
        account.setAccountNumber("ACC-12345678");
        account.setBalance(new BigDecimal("1000.00"));
        account.setOwner(owner);
    }

    // ---------- createAccount() ----------

    @Test
    void createAccount_shouldGenerateAccountNumberAndZeroBalance() {
        User owner = new User();
        owner.setUsername("newuser");

        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.createAccount(owner);

        assertNotNull(result.getAccountNumber());
        assertTrue(result.getAccountNumber().startsWith("ACC-"));
        assertEquals(BigDecimal.ZERO, result.getBalance());
        assertEquals(owner, result.getOwner());
    }

    // ---------- deposit() ----------

    @Test
    void deposit_shouldIncreaseBalance_andRecordTransaction() {
        when(accountRepository.findByAccountNumber("ACC-12345678"))
                .thenReturn(Optional.of(account));

        accountService.deposit("ACC-12345678", new BigDecimal("500.00"));

        assertEquals(new BigDecimal("1500.00"), account.getBalance());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();
        assertEquals(TransactionType.DEPOSIT, saved.getType());
        assertEquals(new BigDecimal("500.00"), saved.getAmount());
    }

    @Test
    void deposit_shouldThrow_whenAmountIsZeroOrNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> accountService.deposit("ACC-12345678", BigDecimal.ZERO));

        verify(accountRepository, never()).findByAccountNumber(any());
    }

    @Test
    void deposit_shouldThrow_whenAccountDoesNotExist() {
        when(accountRepository.findByAccountNumber("ACC-NOTFOUND"))
                .thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.deposit("ACC-NOTFOUND", new BigDecimal("100.00")));
    }

    // ---------- withdraw() ----------

    @Test
    void withdraw_shouldDecreaseBalance_whenFundsAreSufficient() {
        when(accountRepository.findByAccountNumber("ACC-12345678"))
                .thenReturn(Optional.of(account));

        accountService.withdraw("ACC-12345678", new BigDecimal("300.00"));

        assertEquals(new BigDecimal("700.00"), account.getBalance());
        verify(transactionRepository).save(argThat(t ->
                t.getType() == TransactionType.WITHDRAWAL &&
                t.getAmount().compareTo(new BigDecimal("300.00")) == 0));
    }

    @Test
    void withdraw_shouldThrowInsufficientFunds_whenAmountExceedsBalance() {
        when(accountRepository.findByAccountNumber("ACC-12345678"))
                .thenReturn(Optional.of(account));

        assertThrows(InsufficientFundsException.class,
                () -> accountService.withdraw("ACC-12345678", new BigDecimal("9999.00")));

        assertEquals(new BigDecimal("1000.00"), account.getBalance());
        verify(transactionRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void withdraw_shouldAllowExactBalance_asBoundaryCase() {
        when(accountRepository.findByAccountNumber("ACC-12345678"))
                .thenReturn(Optional.of(account));

        accountService.withdraw("ACC-12345678", new BigDecimal("1000.00"));

        assertEquals(0, account.getBalance().compareTo(BigDecimal.ZERO));
    }

    // ---------- transfer() ----------

    @Test
    void transfer_shouldMoveFundsBetweenAccounts() {
        Account destination = new Account();
        destination.setAccountNumber("ACC-87654321");
        destination.setBalance(new BigDecimal("200.00"));

        when(accountRepository.findByAccountNumber("ACC-12345678")).thenReturn(Optional.of(account));
        when(accountRepository.findByAccountNumber("ACC-87654321")).thenReturn(Optional.of(destination));

        accountService.transfer("ACC-12345678", "ACC-87654321", new BigDecimal("300.00"));

        assertEquals(new BigDecimal("700.00"), account.getBalance());
        assertEquals(new BigDecimal("500.00"), destination.getBalance());

        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transfer_shouldThrow_whenSourceHasInsufficientFunds() {
        Account destination = new Account();
        destination.setAccountNumber("ACC-87654321");
        destination.setBalance(new BigDecimal("200.00"));

        when(accountRepository.findByAccountNumber("ACC-12345678")).thenReturn(Optional.of(account));

        assertThrows(InsufficientFundsException.class,
                () -> accountService.transfer("ACC-12345678", "ACC-87654321", new BigDecimal("9999.00")));

        assertEquals(new BigDecimal("200.00"), destination.getBalance());
        verify(accountRepository, never()).findByAccountNumber("ACC-87654321");
    }  
}
