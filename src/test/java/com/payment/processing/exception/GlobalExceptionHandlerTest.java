package com.payment.processing.exception;

import com.payment.processing.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handlePaymentException_BadRequest() {
        PaymentException ex = new PaymentException("Invalid card number");

        ResponseEntity<ErrorResponse> response = handler.handlePaymentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PAYMENT_ERROR", response.getBody().getError());
        assertEquals("Invalid card number", response.getBody().getMessage());
    }

    @Test
    void handlePaymentException_NotFound() {
        PaymentException ex = new PaymentException("Order not found", HttpStatus.NOT_FOUND);

        ResponseEntity<ErrorResponse> response = handler.handlePaymentException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Order not found", response.getBody().getMessage());
    }

    @Test
    void handlePaymentException_PaymentRequired() {
        PaymentException ex = new PaymentException("Card declined", HttpStatus.PAYMENT_REQUIRED);

        ResponseEntity<ErrorResponse> response = handler.handlePaymentException(ex);

        assertEquals(HttpStatus.PAYMENT_REQUIRED, response.getStatusCode());
    }

    @Test
    void handleGenericException() {
        Exception ex = new RuntimeException("Something went wrong");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().getError());
    }
}
