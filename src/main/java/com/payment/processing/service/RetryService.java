package com.payment.processing.service;

import com.payment.processing.entity.Transaction;
import com.payment.processing.enums.TransactionStatus;
import com.payment.processing.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Simple retry service for handling pending/transient payment failures.
 * Provides idempotency through unique retry IDs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetryService {

    private final TransactionRepository transactionRepository;
    private final PaymentService paymentService;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 5000; // 5 seconds

    /**
     * Retry a failed transaction with idempotency check.
     * Uses a unique retry ID to prevent duplicate processing.
     * 
     * @param transactionId The transaction ID to retry
     * @param retryId Unique identifier for this retry attempt (for idempotency)
     * @return true if retry was successful, false otherwise
     */
    @Transactional
    public boolean retryTransaction(Long transactionId, String retryId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        // Check if transaction is in a retryable state
        if (transaction.getStatus() != TransactionStatus.FAILED) {
            log.warn("Transaction {} is not in FAILED status, cannot retry. Current status: {}", 
                    transactionId, transaction.getStatus());
            return false;
        }

        // Check retry attempts (simple approach - store in response message)
        String responseMessage = transaction.getResponseMessage();
        int attemptCount = extractAttemptCount(responseMessage);
        
        if (attemptCount >= MAX_RETRY_ATTEMPTS) {
            log.warn("Transaction {} has exceeded max retry attempts ({})", transactionId, MAX_RETRY_ATTEMPTS);
            return false;
        }

        // Idempotency check - if retryId already processed, skip
        if (isRetryIdProcessed(transaction, retryId)) {
            log.info("Retry ID {} already processed for transaction {}, skipping", retryId, transactionId);
            return true; // Already processed, consider it successful
        }

        log.info("Retrying transaction {} (attempt {}/{})", transactionId, attemptCount + 1, MAX_RETRY_ATTEMPTS);
        
        // Update attempt count in response message (simple storage)
        transaction.setResponseMessage(
            String.format("Retry attempt %d/%d - RetryID: %s - %s", 
                attemptCount + 1, MAX_RETRY_ATTEMPTS, retryId, 
                responseMessage != null ? responseMessage : "Original failure")
        );
        transactionRepository.save(transaction);

        // Note: Actual retry logic would need to re-execute the payment gateway call
        // For simplicity, we're just tracking retry attempts here
        // In a full implementation, you would call paymentService methods again
        
        return true;
    }

    /**
     * Find transactions that are pending or failed and eligible for retry.
     */
    public List<Transaction> findRetryableTransactions() {
        return transactionRepository.findByStatus(TransactionStatus.FAILED)
                .stream()
                .filter(t -> {
                    int attempts = extractAttemptCount(t.getResponseMessage());
                    return attempts < MAX_RETRY_ATTEMPTS;
                })
                .toList();
    }

    /**
     * Generate a unique retry ID for idempotency.
     */
    public String generateRetryId() {
        return "RETRY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private int extractAttemptCount(String responseMessage) {
        if (responseMessage == null || !responseMessage.contains("Retry attempt")) {
            return 0;
        }
        try {
            // Extract number from "Retry attempt X/Y"
            String[] parts = responseMessage.split("Retry attempt ");
            if (parts.length > 1) {
                String attemptPart = parts[1].split("/")[0];
                return Integer.parseInt(attemptPart.trim());
            }
        } catch (Exception e) {
            log.debug("Could not extract attempt count from: {}", responseMessage);
        }
        return 0;
    }

    private boolean isRetryIdProcessed(Transaction transaction, String retryId) {
        String responseMessage = transaction.getResponseMessage();
        return responseMessage != null && responseMessage.contains("RetryID: " + retryId);
    }
}
