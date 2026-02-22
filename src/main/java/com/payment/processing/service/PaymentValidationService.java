package com.payment.processing.service;

import com.payment.processing.entity.Order;
import com.payment.processing.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Service for validating payment business rules and state transitions.
 * Ensures all payment operations follow valid state transitions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentValidationService {

    // Valid state transitions
    private static final Set<String> VALID_FROM_CREATED = Set.of("AUTHORIZED", "CAPTURED");
    private static final Set<String> VALID_FROM_AUTHORIZED = Set.of("CAPTURED", "CANCELLED");
    private static final Set<String> VALID_FROM_CAPTURED = Set.of("CANCELLED", "REFUNDED", "PARTIALLY_REFUNDED");
    private static final Set<String> VALID_FROM_PARTIALLY_REFUNDED = Set.of("REFUNDED");

    // Invalid states for operations
    private static final Set<String> INVALID_FOR_CAPTURE = Set.of("CREATED", "CANCELLED", "REFUNDED", "PARTIALLY_REFUNDED");
    private static final Set<String> INVALID_FOR_REFUND = Set.of("CREATED", "AUTHORIZED", "CANCELLED", "REFUNDED");
    private static final Set<String> INVALID_FOR_CANCEL = Set.of("CREATED", "CANCELLED", "REFUNDED", "PARTIALLY_REFUNDED");

    /**
     * Validates if a state transition is allowed.
     * 
     * @param currentStatus Current order status
     * @param targetStatus Target order status
     * @throws PaymentException if transition is invalid
     */
    public void validateStateTransition(String currentStatus, String targetStatus) {
        if (currentStatus == null) {
            throw new PaymentException("Current status cannot be null", HttpStatus.BAD_REQUEST);
        }

        if (targetStatus == null) {
            throw new PaymentException("Target status cannot be null", HttpStatus.BAD_REQUEST);
        }

        // Same status is always valid (idempotency)
        if (currentStatus.equals(targetStatus)) {
            return;
        }

        boolean isValid = switch (currentStatus) {
            case "CREATED" -> VALID_FROM_CREATED.contains(targetStatus);
            case "AUTHORIZED" -> VALID_FROM_AUTHORIZED.contains(targetStatus);
            case "CAPTURED" -> VALID_FROM_CAPTURED.contains(targetStatus);
            case "PARTIALLY_REFUNDED" -> VALID_FROM_PARTIALLY_REFUNDED.contains(targetStatus);
            default -> false;
        };

        if (!isValid) {
            String errorMessage = String.format(
                "Invalid state transition: Cannot transition from %s to %s",
                currentStatus, targetStatus
            );
            log.warn(errorMessage);
            throw new PaymentException(errorMessage, HttpStatus.BAD_REQUEST);
        }

        log.debug("Valid state transition: {} -> {}", currentStatus, targetStatus);
    }

    /**
     * Validates if capture operation is allowed for the given order status.
     * 
     * @param orderStatus Current order status
     * @throws PaymentException if capture is not allowed
     */
    public void validateCaptureAllowed(String orderStatus) {
        if (INVALID_FOR_CAPTURE.contains(orderStatus)) {
            String errorMessage = String.format(
                "Order is not in AUTHORIZED status, cannot capture. Current status: %s",
                orderStatus
            );
            log.warn(errorMessage);
            throw new PaymentException(errorMessage, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Validates if refund operation is allowed for the given order status.
     * 
     * @param orderStatus Current order status
     * @throws PaymentException if refund is not allowed
     */
    public void validateRefundAllowed(String orderStatus) {
        if (INVALID_FOR_REFUND.contains(orderStatus)) {
            String errorMessage = String.format(
                "Order is not captured, cannot refund. Current status: %s",
                orderStatus
            );
            log.warn(errorMessage);
            throw new PaymentException(errorMessage, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Validates if cancel operation is allowed for the given order status.
     * 
     * @param orderStatus Current order status
     * @throws PaymentException if cancel is not allowed
     */
    public void validateCancelAllowed(String orderStatus) {
        if (INVALID_FOR_CANCEL.contains(orderStatus)) {
            String errorMessage = String.format(
                "Order cannot be cancelled. Current status: %s",
                orderStatus
            );
            log.warn(errorMessage);
            throw new PaymentException(errorMessage, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Validates payment amount.
     * 
     * @param amount Amount to validate
     * @throws PaymentException if amount is invalid
     */
    public void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new PaymentException("Amount cannot be null", HttpStatus.BAD_REQUEST);
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("Amount must be greater than 0", HttpStatus.BAD_REQUEST);
        }

        // Optional: Add maximum amount check
        BigDecimal maxAmount = new BigDecimal("999999.99");
        if (amount.compareTo(maxAmount) > 0) {
            throw new PaymentException(
                String.format("Amount exceeds maximum limit: %s", maxAmount),
                HttpStatus.BAD_REQUEST
            );
        }
    }

    /**
     * Validates refund amount against order amount.
     * 
     * @param refundAmount Refund amount
     * @param orderAmount Order amount
     * @throws PaymentException if refund amount is invalid
     */
    public void validateRefundAmount(BigDecimal refundAmount, BigDecimal orderAmount) {
        validateAmount(refundAmount);

        if (orderAmount == null) {
            throw new PaymentException("Order amount cannot be null", HttpStatus.BAD_REQUEST);
        }

        if (refundAmount.compareTo(orderAmount) > 0) {
            throw new PaymentException(
                String.format("Refund amount (%s) cannot exceed order amount (%s)",
                    refundAmount, orderAmount),
                HttpStatus.BAD_REQUEST
            );
        }
    }

    /**
     * Validates order exists and is in a valid state for the operation.
     * 
     * @param order Order to validate
     * @param requiredStatus Required status (null to skip status check)
     * @throws PaymentException if order is invalid
     */
    public void validateOrder(Order order, String requiredStatus) {
        if (order == null) {
            throw new PaymentException("Order not found", HttpStatus.NOT_FOUND);
        }

        if (requiredStatus != null && !requiredStatus.equals(order.getStatus())) {
            throw new PaymentException(
                String.format("Order is not in %s status. Current status: %s",
                    requiredStatus, order.getStatus()),
                HttpStatus.BAD_REQUEST
            );
        }
    }
}
