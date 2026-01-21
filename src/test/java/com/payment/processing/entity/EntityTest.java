package com.payment.processing.entity;

import com.payment.processing.enums.TransactionStatus;
import com.payment.processing.enums.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EntityTest {

    @Test
    void user_GettersSetters() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("password");
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);

        assertEquals(1L, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("password", user.getPassword());
        assertEquals(now, user.getCreatedAt());
    }

    @Test
    void order_GettersSetters() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderId("ORD-12345");
        order.setAmount(new BigDecimal("100.00"));
        order.setCurrency("USD");
        order.setStatus("CREATED");
        order.setCustomerEmail("test@example.com");
        order.setDescription("Test order");
        LocalDateTime now = LocalDateTime.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        assertEquals(1L, order.getId());
        assertEquals("ORD-12345", order.getOrderId());
        assertEquals(new BigDecimal("100.00"), order.getAmount());
        assertEquals("USD", order.getCurrency());
        assertEquals("CREATED", order.getStatus());
        assertEquals("test@example.com", order.getCustomerEmail());
        assertEquals("Test order", order.getDescription());
    }

    @Test
    void order_DefaultCurrency() {
        Order order = new Order();
        assertEquals("USD", order.getCurrency());
    }

    @Test
    void transaction_GettersSetters() {
        Order order = new Order();
        order.setId(1L);

        Transaction txn = new Transaction();
        txn.setId(1L);
        txn.setOrder(order);
        txn.setTransactionId("TXN-12345");
        txn.setType(TransactionType.PURCHASE);
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setAmount(new BigDecimal("100.00"));
        txn.setAuthCode("ABC123");
        txn.setResponseCode("1");
        txn.setResponseMessage("Approved");
        txn.setCardLastFour("1111");
        txn.setCardType("Visa");

        assertEquals(1L, txn.getId());
        assertEquals(order, txn.getOrder());
        assertEquals("TXN-12345", txn.getTransactionId());
        assertEquals(TransactionType.PURCHASE, txn.getType());
        assertEquals(TransactionStatus.SUCCESS, txn.getStatus());
        assertEquals(new BigDecimal("100.00"), txn.getAmount());
        assertEquals("ABC123", txn.getAuthCode());
        assertEquals("1", txn.getResponseCode());
        assertEquals("Approved", txn.getResponseMessage());
        assertEquals("1111", txn.getCardLastFour());
        assertEquals("Visa", txn.getCardType());
    }
}
