package com.payment.processing.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", unique = true, nullable = false)
    private String orderId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency = "USD";

    @Column(nullable = false)
    private String status;  // CREATED, AUTHORIZED, CAPTURED, CANCELLED, REFUNDED

    @Column(name = "customer_email")
    private String customerEmail;

    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
