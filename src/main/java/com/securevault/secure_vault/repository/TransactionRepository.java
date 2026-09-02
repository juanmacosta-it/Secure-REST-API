package com.securevault.secure_vault.repository;

import com.securevault.securevault.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>{

      List<Transaction> findByAccountIdOrderByTimestampDesc(Long accountId);

    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.amount > :minAmount")
    List<Transaction> findLargeTransactions(@Param("accountId") Long accountId, @Param("minAmount") java.math.BigDecimal minAmount);
}
