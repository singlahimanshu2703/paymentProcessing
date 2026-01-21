package com.payment.processing.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "\\d{13,19}", message = "Invalid card number")
    private String cardNumber;

    @NotBlank(message = "Expiration date is required")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "Expiration date must be in YYYY-MM format")
    private String expirationDate;

    @NotBlank(message = "CVV is required")
    @Pattern(regexp = "\\d{3,4}", message = "Invalid CVV")
    private String cvv;

    @Email(message = "Invalid email")
    private String customerEmail;

    private String description;
}
