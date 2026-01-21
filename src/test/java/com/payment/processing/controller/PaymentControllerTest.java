package com.payment.processing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.processing.dto.PaymentRequest;
import com.payment.processing.dto.PaymentResponse;
import com.payment.processing.security.JwtUtil;
import com.payment.processing.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtUtil jwtUtil;

    private PaymentRequest validRequest;
    private PaymentResponse successResponse;

    @BeforeEach
    void setUp() {
        validRequest = new PaymentRequest();
        validRequest.setAmount(new BigDecimal("100.00"));
        validRequest.setCardNumber("4111111111111111");
        validRequest.setExpirationDate("2025-12");
        validRequest.setCvv("123");
        validRequest.setCustomerEmail("test@example.com");

        successResponse = PaymentResponse.builder()
                .success(true)
                .orderId("ORD-12345678")
                .transactionId("12345")
                .status("CAPTURED")
                .amount(new BigDecimal("100.00"))
                .authCode("ABC123")
                .message("Transaction successful")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser
    void purchase_Success() throws Exception {
        when(paymentService.purchase(any(PaymentRequest.class))).thenReturn(successResponse);

        mockMvc.perform(post("/api/payments/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.orderId").value("ORD-12345678"))
                .andExpect(jsonPath("$.status").value("CAPTURED"));
    }

    @Test
    @WithMockUser
    void authorize_Success() throws Exception {
        successResponse = PaymentResponse.builder()
                .success(true)
                .orderId("ORD-12345678")
                .transactionId("12345")
                .status("AUTHORIZED")
                .amount(new BigDecimal("100.00"))
                .authCode("ABC123")
                .message("Transaction successful")
                .timestamp(LocalDateTime.now())
                .build();

        when(paymentService.authorize(any(PaymentRequest.class))).thenReturn(successResponse);

        mockMvc.perform(post("/api/payments/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));
    }

    @Test
    @WithMockUser
    void capture_Success() throws Exception {
        when(paymentService.capture(eq("ORD-12345678"), any())).thenReturn(successResponse);

        mockMvc.perform(post("/api/payments/capture/ORD-12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    void capture_WithAmount() throws Exception {
        when(paymentService.capture(eq("ORD-12345678"), eq(new BigDecimal("50.00")))).thenReturn(successResponse);

        mockMvc.perform(post("/api/payments/capture/ORD-12345678")
                        .param("amount", "50.00"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void cancel_Success() throws Exception {
        PaymentResponse cancelResponse = PaymentResponse.builder()
                .success(true)
                .orderId("ORD-12345678")
                .status("CANCELLED")
                .message("Transaction voided")
                .timestamp(LocalDateTime.now())
                .build();

        when(paymentService.cancel("ORD-12345678")).thenReturn(cancelResponse);

        mockMvc.perform(post("/api/payments/cancel/ORD-12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser
    void refund_Success() throws Exception {
        PaymentResponse refundResponse = PaymentResponse.builder()
                .success(true)
                .orderId("ORD-12345678")
                .status("REFUNDED")
                .amount(new BigDecimal("100.00"))
                .message("Refund successful")
                .timestamp(LocalDateTime.now())
                .build();

        when(paymentService.refund(eq("ORD-12345678"), any(), any())).thenReturn(refundResponse);

        mockMvc.perform(post("/api/payments/refund/ORD-12345678"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    @WithMockUser
    void refund_Partial() throws Exception {
        PaymentResponse refundResponse = PaymentResponse.builder()
                .success(true)
                .orderId("ORD-12345678")
                .status("PARTIALLY_REFUNDED")
                .amount(new BigDecimal("50.00"))
                .message("Refund successful")
                .timestamp(LocalDateTime.now())
                .build();

        when(paymentService.refund(eq("ORD-12345678"), eq(new BigDecimal("50.00")), any())).thenReturn(refundResponse);

        mockMvc.perform(post("/api/payments/refund/ORD-12345678")
                        .param("amount", "50.00"))
                .andExpect(status().isCreated());
    }

    @Test
    void purchase_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/payments/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void purchase_InvalidRequest_MissingAmount() throws Exception {
        PaymentRequest invalidRequest = new PaymentRequest();
        invalidRequest.setCardNumber("4111111111111111");
        invalidRequest.setExpirationDate("2025-12");
        invalidRequest.setCvv("123");

        mockMvc.perform(post("/api/payments/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void purchase_InvalidRequest_InvalidCardNumber() throws Exception {
        PaymentRequest invalidRequest = new PaymentRequest();
        invalidRequest.setAmount(new BigDecimal("100.00"));
        invalidRequest.setCardNumber("invalid");
        invalidRequest.setExpirationDate("2025-12");
        invalidRequest.setCvv("123");

        mockMvc.perform(post("/api/payments/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
