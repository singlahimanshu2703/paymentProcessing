package com.payment.processing.service;

import com.payment.processing.config.AuthorizeNetConfig;
import com.payment.processing.entity.Order;
import com.payment.processing.entity.Transaction;
import com.payment.processing.enums.TransactionStatus;
import com.payment.processing.enums.TransactionType;
import com.payment.processing.exception.PaymentException;
import com.payment.processing.repository.OrderRepository;
import com.payment.processing.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private AuthorizeNetConfig authorizeNetConfig;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Order testOrder;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderId("ORD-12345678");
        testOrder.setAmount(new BigDecimal("100.00"));
        testOrder.setCurrency("USD");
        testOrder.setStatus("AUTHORIZED");

        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setOrder(testOrder);
        testTransaction.setTransactionId("60123456789");
        testTransaction.setType(TransactionType.AUTHORIZE);
        testTransaction.setStatus(TransactionStatus.SUCCESS);
        testTransaction.setAmount(new BigDecimal("100.00"));
        testTransaction.setCardLastFour("1111");
    }

    // Test capture validation - order not found
    @Test
    void capture_OrderNotFound() {
        when(orderRepository.findByOrderId("INVALID")).thenReturn(Optional.empty());

        PaymentException exception = assertThrows(PaymentException.class, 
                () -> paymentService.capture("INVALID", null));

        assertEquals("Order not found", exception.getMessage());
    }

    // Test capture validation - order not authorized
    @Test
    void capture_OrderNotAuthorized() {
        testOrder.setStatus("CREATED");
        when(orderRepository.findByOrderId("ORD-12345678")).thenReturn(Optional.of(testOrder));

        PaymentException exception = assertThrows(PaymentException.class,
                () -> paymentService.capture("ORD-12345678", null));

        assertEquals("Order is not in AUTHORIZED status", exception.getMessage());
    }

    // Test capture validation - no authorize transaction
    @Test
    void capture_NoAuthTransaction() {
        when(orderRepository.findByOrderId("ORD-12345678")).thenReturn(Optional.of(testOrder));
        when(transactionRepository.findTopByOrderAndTypeOrderByCreatedAtDesc(testOrder, TransactionType.AUTHORIZE))
                .thenReturn(Optional.empty());

        PaymentException exception = assertThrows(PaymentException.class,
                () -> paymentService.capture("ORD-12345678", null));

        assertEquals("Authorization transaction not found", exception.getMessage());
    }

    // Test cancel validation - order not found
    @Test
    void cancel_OrderNotFound() {
        when(orderRepository.findByOrderId("INVALID")).thenReturn(Optional.empty());

        PaymentException exception = assertThrows(PaymentException.class,
                () -> paymentService.cancel("INVALID"));

        assertEquals("Order not found", exception.getMessage());
    }

    // Test cancel validation - order already cancelled
    @Test
    void cancel_OrderAlreadyCancelled() {
        testOrder.setStatus("CANCELLED");
        when(orderRepository.findByOrderId("ORD-12345678")).thenReturn(Optional.of(testOrder));

        PaymentException exception = assertThrows(PaymentException.class,
                () -> paymentService.cancel("ORD-12345678"));

        assertEquals("Order cannot be cancelled", exception.getMessage());
    }

    // Test cancel validation - order refunded
    @Test
    void cancel_OrderRefunded() {
        testOrder.setStatus("REFUNDED");
        when(orderRepository.findByOrderId("ORD-12345678")).thenReturn(Optional.of(testOrder));

        PaymentException exception = assertThrows(PaymentException.class,
                () -> paymentService.cancel("ORD-12345678"));

        assertEquals("Order cannot be cancelled", exception.getMessage());
    }

    // Test refund validation - order not found
    @Test
    void refund_OrderNotFound() {
        when(orderRepository.findByOrderId("INVALID")).thenReturn(Optional.empty());

        PaymentException exception = assertThrows(PaymentException.class,
                () -> paymentService.refund("INVALID", null, null));

        assertEquals("Order not found", exception.getMessage());
    }

    // Test refund validation - order not captured
    @Test
    void refund_OrderNotCaptured() {
        testOrder.setStatus("AUTHORIZED");
        when(orderRepository.findByOrderId("ORD-12345678")).thenReturn(Optional.of(testOrder));

        PaymentException exception = assertThrows(PaymentException.class,
                () -> paymentService.refund("ORD-12345678", null, null));

        assertEquals("Order is not captured, cannot refund", exception.getMessage());
    }

    // Test refund validation - no capture transaction found
    @Test
    void refund_NoCaptureTransaction() {
        testOrder.setStatus("CAPTURED");
        when(orderRepository.findByOrderId("ORD-12345678")).thenReturn(Optional.of(testOrder));
        when(transactionRepository.findByOrder(testOrder)).thenReturn(List.of());

        PaymentException exception = assertThrows(PaymentException.class,
                () -> paymentService.refund("ORD-12345678", null, null));

        assertEquals("Capture transaction not found", exception.getMessage());
    }

    // Test order status validation
    @Test
    void cancel_ValidatesAuthorizedStatus() {
        testOrder.setStatus("AUTHORIZED");
        when(orderRepository.findByOrderId("ORD-12345678")).thenReturn(Optional.of(testOrder));
        when(transactionRepository.findByOrder(testOrder)).thenReturn(List.of(testTransaction));

        // Will fail at gateway call but validates status check passed
        when(authorizeNetConfig.getApiLoginId()).thenReturn("test");
        when(authorizeNetConfig.getTransactionKey()).thenReturn("test");
        when(authorizeNetConfig.isSandbox()).thenReturn(true);

        // The actual gateway call will fail, but we've validated the status checks work
        assertDoesNotThrow(() -> {
            try {
                paymentService.cancel("ORD-12345678");
            } catch (PaymentException e) {
                // Expected - gateway returns null in test
                if (!e.getMessage().contains("Order cannot be cancelled")) {
                    return; // Test passed - got past validation
                }
                throw e;
            }
        });
    }

    // Test that captured orders can be cancelled
    @Test
    void cancel_ValidatesCapturedStatus() {
        testOrder.setStatus("CAPTURED");
        Transaction captureTxn = new Transaction();
        captureTxn.setType(TransactionType.CAPTURE);
        captureTxn.setStatus(TransactionStatus.SUCCESS);
        captureTxn.setTransactionId("123456");
        captureTxn.setOrder(testOrder);

        when(orderRepository.findByOrderId("ORD-12345678")).thenReturn(Optional.of(testOrder));
        when(transactionRepository.findByOrder(testOrder)).thenReturn(List.of(captureTxn));
        when(authorizeNetConfig.getApiLoginId()).thenReturn("test");
        when(authorizeNetConfig.getTransactionKey()).thenReturn("test");
        when(authorizeNetConfig.isSandbox()).thenReturn(true);

        // Will fail at gateway but validates we got past status check
        assertDoesNotThrow(() -> {
            try {
                paymentService.cancel("ORD-12345678");
            } catch (PaymentException e) {
                if (!e.getMessage().contains("Order cannot be cancelled")) {
                    return;
                }
                throw e;
            }
        });
    }
}
