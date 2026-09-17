package com.securevault.secure_vault.controller;

import com.securevault.secure_vault.dto.AccountResponse;
import com.securevault.secure_vault.dto.TransactionRequest;
import com.securevault.secure_vault.dto.TransferRequest;
import com.securevault.secure_vault.model.Account;
import com.securevault.secure_vault.model.User;
import com.securevault.secure_vault.repository.UserRepository;
import com.securevault.secure_vault.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

      private final AccountService accountService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(Authentication authentication) { // (1)
        User owner = currentUser(authentication);
        Account account = accountService.createAccount(owner);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AccountResponse(account.getAccountNumber(), account.getBalance()));
    }

    @PostMapping("/deposit")
    public ResponseEntity<Void> deposit(@jakarta.validation.Valid @RequestBody TransactionRequest request) {
        accountService.deposit(request.accountNumber(), request.amount());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Void> withdraw(@jakarta.validation.Valid @RequestBody TransactionRequest request) {
        accountService.withdraw(request.accountNumber(), request.amount());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(@jakarta.validation.Valid @RequestBody TransferRequest request) {
        accountService.transfer(request.fromAccountNumber(), request.toAccountNumber(), request.amount());
        return ResponseEntity.ok().build();
    }

    private User currentUser(Authentication authentication) {
        String username = authentication.getName();                  // (2)
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
    }
}
