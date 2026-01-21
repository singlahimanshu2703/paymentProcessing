package com.payment.processing.repository;

import com.payment.processing.entity.Order;
import com.payment.processing.entity.Transaction;
import com.payment.processing.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByOrder(Order order);
    Optional<Transaction> findByOrderAndType(Order order, TransactionType type);
    Optional<Transaction> findTopByOrderAndTypeOrderByCreatedAtDesc(Order order, TransactionType type);
}
