package com.payment.processing.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private boolean success;
    private String orderId;
    private String transactionId;
    private String status;
    private BigDecimal amount;
    private String authCode;
    private String message;
    private LocalDateTime timestamp;
}
