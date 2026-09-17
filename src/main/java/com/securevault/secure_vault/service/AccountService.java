
package com.securevault.secure_vault.service;

import com.securevault.secure_vault.exception.AccountNotFoundException;
import com.securevault.secure_vault.exception.InsufficientFundsException;
import com.securevault.secure_vault.model.Account;
import com.securevault.secure_vault.model.Transaction;
import com.securevault.secure_vault.model.TransactionType;
import com.securevault.secure_vault.model.User;
import com.securevault.secure_vault.repository.AccountRepository;
import com.securevault.secure_vault.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

     private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public Account createAccount(User owner) {
        Account account = new Account();
        account.setOwner(owner);
        account.setAccountNumber(generateAccountNumber());            // (1)
        account.setBalance(BigDecimal.ZERO);
        return accountRepository.save(account);
    }

    @Transactional
    public void deposit(String accountNumber, BigDecimal amount) {
        validateAmount(amount);                                       // (2)
        Account account = findAccountOrThrow(accountNumber);

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        recordTransaction(account, TransactionType.DEPOSIT, amount);

        log.info("Deposit of {} to account {}", amount, accountNumber);
    }

    @Transactional
    public void withdraw(String accountNumber, BigDecimal amount) {
        validateAmount(amount);
        Account account = findAccountOrThrow(accountNumber);

        if (account.getBalance().compareTo(amount) < 0) {              // (3)
            log.warn("Insufficient funds for withdrawal on account {}", accountNumber);
            throw new InsufficientFundsException("Insufficient balance for this withdrawal");
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        recordTransaction(account, TransactionType.WITHDRAWAL, amount);

        log.info("Withdrawal of {} from account {}", amount, accountNumber);
    }

    @Transactional
    public void transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        withdraw(fromAccountNumber, amount);                           // (4)
        deposit(toAccountNumber, amount);
        log.info("Transfer of {} from {} to {}", amount, fromAccountNumber, toAccountNumber);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {                  // (5)
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private Account findAccountOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
    }

    private void recordTransaction(Account account, TransactionType type, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(type);
        transaction.setAmount(amount);
        transactionRepository.save(transaction);
    }

    private String generateAccountNumber() {
        return "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(); // (6)
    }
}
