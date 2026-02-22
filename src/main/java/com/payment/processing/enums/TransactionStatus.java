package com.payment.processing.enums;

public enum TransactionStatus {
    SUCCESS,
    FAILED,
    PENDING  // For transactions awaiting retry or async processing
}
