package com.payment.processing.service;

import com.payment.processing.config.AuthorizeNetConfig;
import com.payment.processing.dto.PaymentRequest;
import com.payment.processing.dto.PaymentResponse;
import com.payment.processing.entity.Order;
import com.payment.processing.entity.Transaction;
import com.payment.processing.enums.TransactionStatus;
import com.payment.processing.enums.TransactionType;
import com.payment.processing.exception.PaymentException;
import com.payment.processing.repository.OrderRepository;
import com.payment.processing.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.authorize.Environment;
import net.authorize.api.contract.v1.*;
import net.authorize.api.controller.*;
import net.authorize.api.controller.base.ApiOperationBase;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final AuthorizeNetConfig authorizeNetConfig;
    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentValidationService validationService;

    private MerchantAuthenticationType getMerchantAuth() {
        MerchantAuthenticationType merchantAuth = new MerchantAuthenticationType();
        merchantAuth.setName(authorizeNetConfig.getApiLoginId());
        merchantAuth.setTransactionKey(authorizeNetConfig.getTransactionKey());
        ApiOperationBase.setEnvironment(authorizeNetConfig.isSandbox() ? Environment.SANDBOX : Environment.PRODUCTION);
        return merchantAuth;
    }

    private CreditCardType createCreditCard(PaymentRequest request) {
        CreditCardType creditCard = new CreditCardType();
        creditCard.setCardNumber(request.getCardNumber());
        creditCard.setExpirationDate(request.getExpirationDate());
        creditCard.setCardCode(request.getCvv());
        return creditCard;
    }

    @Transactional(noRollbackFor = PaymentException.class)
    public PaymentResponse purchase(PaymentRequest request) {
        // Validate amount
        validationService.validateAmount(request.getAmount());
        
        // Create order
        Order order = createOrder(request);

        // Create Authorize.Net transaction
        TransactionRequestType txnRequest = new TransactionRequestType();
        txnRequest.setTransactionType(TransactionTypeEnum.AUTH_CAPTURE_TRANSACTION.value());
        txnRequest.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));

        PaymentType payment = new PaymentType();
        payment.setCreditCard(createCreditCard(request));
        txnRequest.setPayment(payment);

        // Execute transaction
        CreateTransactionRequest apiRequest = new CreateTransactionRequest();
        apiRequest.setMerchantAuthentication(getMerchantAuth());
        apiRequest.setTransactionRequest(txnRequest);

        CreateTransactionController controller = new CreateTransactionController(apiRequest);
        controller.execute();

        CreateTransactionResponse response = controller.getApiResponse();
        return processTransactionResponse(response, order, TransactionType.PURCHASE, request.getAmount());
    }

    @Transactional(noRollbackFor = PaymentException.class)
    public PaymentResponse authorize(PaymentRequest request) {
        // Validate amount
        validationService.validateAmount(request.getAmount());
        
        // Create order
        Order order = createOrder(request);
        order.setStatus("AUTHORIZED");

        // Create Authorize.Net transaction
        TransactionRequestType txnRequest = new TransactionRequestType();
        txnRequest.setTransactionType(TransactionTypeEnum.AUTH_ONLY_TRANSACTION.value());
        txnRequest.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));

        PaymentType payment = new PaymentType();
        payment.setCreditCard(createCreditCard(request));
        txnRequest.setPayment(payment);

        CreateTransactionRequest apiRequest = new CreateTransactionRequest();
        apiRequest.setMerchantAuthentication(getMerchantAuth());
        apiRequest.setTransactionRequest(txnRequest);

        CreateTransactionController controller = new CreateTransactionController(apiRequest);
        controller.execute();

        CreateTransactionResponse response = controller.getApiResponse();
        return processTransactionResponse(response, order, TransactionType.AUTHORIZE, request.getAmount());
    }

    @Transactional(noRollbackFor = PaymentException.class)
    public PaymentResponse capture(String orderId, BigDecimal amount) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentException("Order not found", HttpStatus.NOT_FOUND));

        // Validate order and state transition
        validationService.validateOrder(order, null);
        validationService.validateCaptureAllowed(order.getStatus());
        
        // Validate amount if provided
        if (amount != null) {
            validationService.validateAmount(amount);
        }

        // Get the authorize transaction
        Transaction authTxn = transactionRepository.findTopByOrderAndTypeOrderByCreatedAtDesc(order, TransactionType.AUTHORIZE)
                .orElseThrow(() -> new PaymentException("Authorization transaction not found", HttpStatus.NOT_FOUND));

        BigDecimal captureAmount = amount != null ? amount : order.getAmount();

        // Create capture transaction
        TransactionRequestType txnRequest = new TransactionRequestType();
        txnRequest.setTransactionType(TransactionTypeEnum.PRIOR_AUTH_CAPTURE_TRANSACTION.value());
        txnRequest.setAmount(captureAmount.setScale(2, RoundingMode.HALF_UP));
        txnRequest.setRefTransId(authTxn.getTransactionId());

        CreateTransactionRequest apiRequest = new CreateTransactionRequest();
        apiRequest.setMerchantAuthentication(getMerchantAuth());
        apiRequest.setTransactionRequest(txnRequest);

        CreateTransactionController controller = new CreateTransactionController(apiRequest);
        controller.execute();

        CreateTransactionResponse response = controller.getApiResponse();
        order.setStatus("CAPTURED");
        return processTransactionResponse(response, order, TransactionType.CAPTURE, captureAmount);
    }

    @Transactional(noRollbackFor = PaymentException.class)
    public PaymentResponse cancel(String orderId) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentException("Order not found", HttpStatus.NOT_FOUND));

        // Validate order and state transition
        validationService.validateOrder(order, null);
        validationService.validateCancelAllowed(order.getStatus());

        // Get the last transaction
        Transaction lastTxn = transactionRepository.findByOrder(order).stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .reduce((first, second) -> second)
                .orElseThrow(() -> new PaymentException("No successful transaction found", HttpStatus.NOT_FOUND));

        // Create void transaction
        TransactionRequestType txnRequest = new TransactionRequestType();
        txnRequest.setTransactionType(TransactionTypeEnum.VOID_TRANSACTION.value());
        txnRequest.setRefTransId(lastTxn.getTransactionId());

        CreateTransactionRequest apiRequest = new CreateTransactionRequest();
        apiRequest.setMerchantAuthentication(getMerchantAuth());
        apiRequest.setTransactionRequest(txnRequest);

        CreateTransactionController controller = new CreateTransactionController(apiRequest);
        controller.execute();

        CreateTransactionResponse response = controller.getApiResponse();
        order.setStatus("CANCELLED");
        return processTransactionResponse(response, order, TransactionType.VOID, order.getAmount());
    }

    @Transactional(noRollbackFor = PaymentException.class)
    public PaymentResponse refund(String orderId, BigDecimal amount, String cardLastFour) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentException("Order not found", HttpStatus.NOT_FOUND));

        // Validate order and state transition
        validationService.validateOrder(order, null);
        validationService.validateRefundAllowed(order.getStatus());
        
        // Validate refund amount
        BigDecimal refundAmount = amount != null ? amount : order.getAmount();
        validationService.validateRefundAmount(refundAmount, order.getAmount());

        // Get the capture/purchase transaction
        Transaction captureTxn = transactionRepository.findByOrder(order).stream()
                .filter(t -> (t.getType() == TransactionType.CAPTURE || t.getType() == TransactionType.PURCHASE) 
                        && t.getStatus() == TransactionStatus.SUCCESS)
                .findFirst()
                .orElseThrow(() -> new PaymentException("Capture transaction not found", HttpStatus.NOT_FOUND));

        // Create refund transaction
        TransactionRequestType txnRequest = new TransactionRequestType();
        txnRequest.setTransactionType(TransactionTypeEnum.REFUND_TRANSACTION.value());
        txnRequest.setAmount(refundAmount.setScale(2, RoundingMode.HALF_UP));
        txnRequest.setRefTransId(captureTxn.getTransactionId());

        // For refund, we need the last 4 digits of the card
        CreditCardType creditCard = new CreditCardType();
        String lastFour = cardLastFour != null ? cardLastFour : captureTxn.getCardLastFour();
        creditCard.setCardNumber(lastFour);
        creditCard.setExpirationDate("XXXX");

        PaymentType payment = new PaymentType();
        payment.setCreditCard(creditCard);
        txnRequest.setPayment(payment);

        CreateTransactionRequest apiRequest = new CreateTransactionRequest();
        apiRequest.setMerchantAuthentication(getMerchantAuth());
        apiRequest.setTransactionRequest(txnRequest);

        CreateTransactionController controller = new CreateTransactionController(apiRequest);
        controller.execute();

        CreateTransactionResponse response = controller.getApiResponse();
        
        // Update order status based on refund amount
        if (refundAmount.compareTo(order.getAmount()) >= 0) {
            order.setStatus("REFUNDED");
        } else {
            order.setStatus("PARTIALLY_REFUNDED");
        }
        
        return processTransactionResponse(response, order, TransactionType.REFUND, refundAmount);
    }

    private Order createOrder(PaymentRequest request) {
        Order order = new Order();
        order.setOrderId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setAmount(request.getAmount());
        order.setCurrency("USD");
        order.setStatus("CREATED");
        order.setCustomerEmail(request.getCustomerEmail());
        order.setDescription(request.getDescription());
        return orderRepository.save(order);
    }

    private PaymentResponse processTransactionResponse(CreateTransactionResponse response, Order order, 
                                                       TransactionType type, BigDecimal amount) {
        Transaction txn = new Transaction();
        txn.setOrder(order);
        txn.setType(type);
        txn.setAmount(amount);
        txn.setCreatedAt(LocalDateTime.now());

        // Log API response for debugging
        if (response != null) {
            log.info("Authorize.Net Response - Result: {}", 
                    response.getMessages() != null ? response.getMessages().getResultCode() : "null");
            if (response.getMessages() != null && response.getMessages().getMessage() != null) {
                response.getMessages().getMessage().forEach(m -> 
                    log.info("Message: {} - {}", m.getCode(), m.getText()));
            }
        }

        if (response != null && response.getTransactionResponse() != null) {
            TransactionResponse txnResponse = response.getTransactionResponse();
            log.info("Transaction Response Code: {}", txnResponse.getResponseCode());

            if ("1".equals(txnResponse.getResponseCode())) {
                // Success
                txn.setStatus(TransactionStatus.SUCCESS);
                txn.setTransactionId(txnResponse.getTransId());
                txn.setAuthCode(txnResponse.getAuthCode());
                txn.setResponseCode(txnResponse.getResponseCode());
                txn.setResponseMessage("Transaction successful");
                
                if (txnResponse.getAccountNumber() != null) {
                    txn.setCardLastFour(txnResponse.getAccountNumber().substring(
                            Math.max(0, txnResponse.getAccountNumber().length() - 4)));
                }
                txn.setCardType(txnResponse.getAccountType());

                // Validate and set status with state transition validation
                String newStatus = type == TransactionType.AUTHORIZE ? "AUTHORIZED" : 
                                 type == TransactionType.VOID ? "CANCELLED" :
                                 type == TransactionType.REFUND ? order.getStatus() : "CAPTURED";
                
                if (!newStatus.equals(order.getStatus())) {
                    validationService.validateStateTransition(order.getStatus(), newStatus);
                }
                order.setStatus(newStatus);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                transactionRepository.save(txn);

                return PaymentResponse.builder()
                        .success(true)
                        .orderId(order.getOrderId())
                        .transactionId(txnResponse.getTransId())
                        .status(order.getStatus())
                        .amount(amount)
                        .authCode(txnResponse.getAuthCode())
                        .message("Transaction successful")
                        .timestamp(LocalDateTime.now())
                        .build();
            } else {
                // Failed
                txn.setStatus(TransactionStatus.FAILED);
                txn.setResponseCode(txnResponse.getResponseCode());
                String errorMessage = "Transaction failed";
                if (txnResponse.getErrors() != null && !txnResponse.getErrors().getError().isEmpty()) {
                    errorMessage = txnResponse.getErrors().getError().get(0).getErrorText();
                }
                txn.setResponseMessage(errorMessage);
                transactionRepository.save(txn);

                throw new PaymentException(errorMessage, HttpStatus.PAYMENT_REQUIRED);
            }
        } else {
            // No response
            txn.setStatus(TransactionStatus.FAILED);
            String errorMessage = "No response from payment gateway";
            if (response != null && response.getMessages() != null 
                    && response.getMessages().getMessage() != null 
                    && !response.getMessages().getMessage().isEmpty()) {
                errorMessage = response.getMessages().getMessage().get(0).getText();
            }
            txn.setResponseMessage(errorMessage);
            transactionRepository.save(txn);

            throw new PaymentException(errorMessage, HttpStatus.BAD_GATEWAY);
        }
    }
}
