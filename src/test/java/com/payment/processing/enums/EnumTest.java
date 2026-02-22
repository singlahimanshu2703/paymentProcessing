package com.payment.processing.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnumTest {

    @Test
    void transactionType_Values() {
        assertEquals(5, TransactionType.values().length);
        
        assertNotNull(TransactionType.PURCHASE);
        assertNotNull(TransactionType.AUTHORIZE);
        assertNotNull(TransactionType.CAPTURE);
        assertNotNull(TransactionType.VOID);
        assertNotNull(TransactionType.REFUND);
    }

    @Test
    void transactionType_ValueOf() {
        assertEquals(TransactionType.PURCHASE, TransactionType.valueOf("PURCHASE"));
        assertEquals(TransactionType.AUTHORIZE, TransactionType.valueOf("AUTHORIZE"));
        assertEquals(TransactionType.CAPTURE, TransactionType.valueOf("CAPTURE"));
        assertEquals(TransactionType.VOID, TransactionType.valueOf("VOID"));
        assertEquals(TransactionType.REFUND, TransactionType.valueOf("REFUND"));
    }

    @Test
    void transactionStatus_Values() {
        assertEquals(3, TransactionStatus.values().length);
        
        assertNotNull(TransactionStatus.SUCCESS);
        assertNotNull(TransactionStatus.FAILED);
        assertNotNull(TransactionStatus.PENDING);
    }

    @Test
    void transactionStatus_ValueOf() {
        assertEquals(TransactionStatus.SUCCESS, TransactionStatus.valueOf("SUCCESS"));
        assertEquals(TransactionStatus.FAILED, TransactionStatus.valueOf("FAILED"));
        assertEquals(TransactionStatus.PENDING, TransactionStatus.valueOf("PENDING"));
    }
}
