package com.payment.processing.controller;

import com.payment.processing.dto.PaymentRequest;
import com.payment.processing.dto.PaymentResponse;
import com.payment.processing.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/purchase")
    public ResponseEntity<PaymentResponse> purchase(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.purchase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/authorize")
    public ResponseEntity<PaymentResponse> authorize(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.authorize(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/capture/{orderId}")
    public ResponseEntity<PaymentResponse> capture(
            @PathVariable String orderId,
            @RequestParam(required = false) BigDecimal amount) {
        PaymentResponse response = paymentService.capture(orderId, amount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cancel/{orderId}")
    public ResponseEntity<PaymentResponse> cancel(@PathVariable String orderId) {
        PaymentResponse response = paymentService.cancel(orderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refund/{orderId}")
    public ResponseEntity<PaymentResponse> refund(
            @PathVariable String orderId,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) String cardLastFour) {
        PaymentResponse response = paymentService.refund(orderId, amount, cardLastFour);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
