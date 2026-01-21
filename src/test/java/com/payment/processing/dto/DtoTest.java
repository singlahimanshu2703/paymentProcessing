package com.payment.processing.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    @Test
    void loginRequest_GettersSetters() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password");

        assertEquals("testuser", request.getUsername());
        assertEquals("password", request.getPassword());
    }

    @Test
    void paymentRequest_GettersSetters() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setCardNumber("4111111111111111");
        request.setExpirationDate("2025-12");
        request.setCvv("123");
        request.setCustomerEmail("test@example.com");
        request.setDescription("Test payment");

        assertEquals(new BigDecimal("100.00"), request.getAmount());
        assertEquals("4111111111111111", request.getCardNumber());
        assertEquals("2025-12", request.getExpirationDate());
        assertEquals("123", request.getCvv());
        assertEquals("test@example.com", request.getCustomerEmail());
        assertEquals("Test payment", request.getDescription());
    }

    @Test
    void paymentResponse_Builder() {
        LocalDateTime now = LocalDateTime.now();
        
        PaymentResponse response = PaymentResponse.builder()
                .success(true)
                .orderId("ORD-12345")
                .transactionId("TXN-67890")
                .status("CAPTURED")
                .amount(new BigDecimal("100.00"))
                .authCode("ABC123")
                .message("Success")
                .timestamp(now)
                .build();

        assertTrue(response.isSuccess());
        assertEquals("ORD-12345", response.getOrderId());
        assertEquals("TXN-67890", response.getTransactionId());
        assertEquals("CAPTURED", response.getStatus());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertEquals("ABC123", response.getAuthCode());
        assertEquals("Success", response.getMessage());
        assertEquals(now, response.getTimestamp());
    }

    @Test
    void errorResponse_Constructor() {
        ErrorResponse error = new ErrorResponse("ERROR_CODE", "Error message");

        assertEquals("ERROR_CODE", error.getError());
        assertEquals("Error message", error.getMessage());
        assertNotNull(error.getTimestamp());
    }

    @Test
    void errorResponse_FullConstructor() {
        LocalDateTime time = LocalDateTime.now();
        ErrorResponse error = new ErrorResponse("ERROR_CODE", "Error message", time);

        assertEquals("ERROR_CODE", error.getError());
        assertEquals("Error message", error.getMessage());
        assertEquals(time, error.getTimestamp());
    }
}
