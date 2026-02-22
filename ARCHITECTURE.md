# Payment Processing System - Architecture

## Overview

The Payment Processing System is a Spring Boot application that integrates with Authorize.Net to process credit card payments. It provides a RESTful API for payment operations with JWT-based authentication, transaction tracking, and retry mechanisms.

## System Architecture

```
┌─────────────┐
│   Client    │
│ Application │
└──────┬──────┘
       │ HTTPS/REST
       │ JWT Token
       ▼
┌─────────────────────────────────────┐
│     Payment Processing API          │
│  ┌──────────────────────────────┐ │
│  │  Spring Boot Application       │ │
│  │  ┌──────────────────────────┐ │ │
│  │  │  REST Controllers         │ │ │
│  │  │  - PaymentController      │ │ │
│  │  │  - AuthController         │ │ │
│  │  └──────────┬───────────────┘ │ │
│  │             │                  │ │
│  │  ┌──────────▼───────────────┐ │ │
│  │  │  Service Layer            │ │ │
│  │  │  - PaymentService         │ │ │
│  │  │  - RetryService           │ │ │
│  │  │  - PaymentValidationService│ │ │
│  │  └──────────┬───────────────┘ │ │
│  │             │                  │ │
│  │  ┌──────────▼───────────────┐ │ │
│  │  │  Repository Layer          │ │ │
│  │  │  - OrderRepository        │ │ │
│  │  │  - TransactionRepository  │ │ │
│  │  └──────────┬───────────────┘ │ │
│  └─────────────┼──────────────────┘ │
│                 │                    │
└─────────────────┼────────────────────┘
                  │
        ┌─────────┴─────────┐
        │                   │
        ▼                   ▼
┌──────────────┐    ┌──────────────┐
│   MySQL      │    │ Authorize.Net│
│   Database   │    │   Gateway    │
└──────────────┘    └──────────────┘
```

## Payment Flow Diagrams

### 1. Purchase Flow (Authorize + Capture in one step)

```
Client                    API                    Database              Authorize.Net
  │                        │                        │                        │
  │──POST /purchase───────▶│                        │                        │
  │                        │                        │                        │
  │                        │──Create Order─────────▶│                        │
  │                        │◀──Order Created────────│                        │
  │                        │                        │                        │
  │                        │──Auth + Capture───────┼───────────────────────▶│
  │                        │                        │                        │
  │                        │◀───────────────────────┼──Transaction Response──│
  │                        │                        │                        │
  │                        │──Save Transaction─────▶│                        │
  │                        │──Update Order─────────▶│                        │
  │                        │                        │                        │
  │◀──201 Created──────────│                        │                        │
  │   (PaymentResponse)    │                        │                        │
```

### 2. Authorize Flow

```
Client                    API                    Database              Authorize.Net
  │                        │                        │                        │
  │──POST /authorize───────▶│                        │                        │
  │                        │                        │                        │
  │                        │──Create Order─────────▶│                        │
  │                        │◀──Order Created────────│                        │
  │                        │                        │                        │
  │                        │──Authorize Only───────┼───────────────────────▶│
  │                        │                        │                        │
  │                        │◀───────────────────────┼──Authorization Response│
  │                        │                        │                        │
  │                        │──Save Transaction─────▶│                        │
  │                        │──Update Order─────────▶│                        │
  │                        │   (AUTHORIZED)          │                        │
  │                        │                        │                        │
  │◀──201 Created──────────│                        │                        │
  │   (PaymentResponse)    │                        │                        │
```

### 3. Capture Flow

```
Client                    API                    Database              Authorize.Net
  │                        │                        │                        │
  │──POST /capture/{id}───▶│                        │                        │
  │                        │                        │                        │
  │                        │──Validate Order───────▶│                        │
  │                        │   (must be AUTHORIZED) │                        │
  │                        │                        │                        │
  │                        │──Get Auth Transaction─▶│                        │
  │                        │                        │                        │
  │                        │──Capture───────────────┼───────────────────────▶│
  │                        │                        │                        │
  │                        │◀───────────────────────┼──Capture Response──────│
  │                        │                        │                        │
  │                        │──Save Transaction─────▶│                        │
  │                        │──Update Order─────────▶│                        │
  │                        │   (CAPTURED)           │                        │
  │                        │                        │                        │
  │◀──200 OK───────────────│                        │                        │
  │   (PaymentResponse)    │                        │                        │
```

### 4. Refund Flow

```
Client                    API                    Database              Authorize.Net
  │                        │                        │                        │
  │──POST /refund/{id}────▶│                        │                        │
  │                        │                        │                        │
  │                        │──Validate Order───────▶│                        │
  │                        │   (must be CAPTURED)   │                        │
  │                        │                        │                        │
  │                        │──Get Capture Txn───────▶│                        │
  │                        │                        │                        │
  │                        │──Refund───────────────┼───────────────────────▶│
  │                        │                        │                        │
  │                        │◀───────────────────────┼──Refund Response───────│
  │                        │                        │                        │
  │                        │──Save Transaction─────▶│                        │
  │                        │──Update Order─────────▶│                        │
  │                        │   (REFUNDED or         │                        │
  │                        │    PARTIALLY_REFUNDED)  │                        │
  │                        │                        │                        │
  │◀──201 Created──────────│                        │                        │
  │   (PaymentResponse)    │                        │                        │
```

### 5. Cancel Flow

```
Client                    API                    Database              Authorize.Net
  │                        │                        │                        │
  │──POST /cancel/{id}────▶│                        │                        │
  │                        │                        │                        │
  │                        │──Validate Order───────▶│                        │
  │                        │   (AUTHORIZED or       │                        │
  │                        │    CAPTURED)           │                        │
  │                        │                        │                        │
  │                        │──Get Last Transaction─▶│                        │
  │                        │                        │                        │
  │                        │──Void─────────────────┼───────────────────────▶│
  │                        │                        │                        │
  │                        │◀───────────────────────┼──Void Response─────│
  │                        │                        │                        │                        │
  │                        │──Save Transaction─────▶│                        │
  │                        │──Update Order─────────▶│                        │
  │                        │   (CANCELLED)          │                        │
  │                        │                        │                        │
  │◀──200 OK───────────────│                        │                        │
  │   (PaymentResponse)    │                        │                        │
```

## Database Schema

### Entity Relationship Diagram

```
┌─────────────┐         ┌──────────────┐
│    Users    │         │    Orders    │
├─────────────┤         ├──────────────┤
│ id (PK)     │         │ id (PK)      │
│ username    │         │ order_id (UK)│
│ password    │         │ amount       │
│ created_at  │         │ currency     │
└─────────────┘         │ status       │
                        │ customer_email│
                        │ description  │
                        │ created_at  │
                        │ updated_at  │
                        └──────┬───────┘
                               │
                               │ 1:N
                               │
                        ┌──────▼──────────┐
                        │  Transactions  │
                        ├────────────────┤
                        │ id (PK)        │
                        │ order_id (FK)  │
                        │ transaction_id │
                        │ type           │
                        │ status         │
                        │ amount         │
                        │ auth_code      │
                        │ response_code  │
                        │ response_message│
                        │ card_last_four │
                        │ card_type      │
                        │ created_at    │
                        └────────────────┘
```

### Tables

#### users
Stores user accounts for authentication.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique identifier |
| username | VARCHAR(50) | UNIQUE, NOT NULL | Username for login |
| password | VARCHAR(255) | NOT NULL | Encrypted password |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Account creation time |

#### orders
Stores payment orders.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique identifier |
| order_id | VARCHAR(50) | UNIQUE, NOT NULL | Business order identifier (e.g., ORD-ABC12345) |
| amount | DECIMAL(10,2) | NOT NULL | Order amount |
| currency | VARCHAR(3) | DEFAULT 'USD' | Currency code |
| status | VARCHAR(20) | NOT NULL | Order status (CREATED, AUTHORIZED, CAPTURED, CANCELLED, REFUNDED, PARTIALLY_REFUNDED) |
| customer_email | VARCHAR(100) | | Customer email address |
| description | VARCHAR(255) | | Order description |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Order creation time |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP ON UPDATE | Last update time |

#### transactions
Stores payment transactions.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique identifier |
| order_id | BIGINT | FOREIGN KEY (orders.id), NOT NULL | Reference to order |
| transaction_id | VARCHAR(50) | | Authorize.Net transaction ID |
| type | VARCHAR(20) | NOT NULL | Transaction type (PURCHASE, AUTHORIZE, CAPTURE, VOID, REFUND) |
| status | VARCHAR(20) | NOT NULL | Transaction status (SUCCESS, FAILED, PENDING) |
| amount | DECIMAL(10,2) | NOT NULL | Transaction amount |
| auth_code | VARCHAR(20) | | Authorization code from gateway |
| response_code | VARCHAR(10) | | Gateway response code |
| response_message | TEXT | | Gateway response message |
| card_last_four | VARCHAR(4) | | Last 4 digits of card |
| card_type | VARCHAR(20) | | Card type (Visa, MasterCard, etc.) |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Transaction creation time |

## State Management

### Order State Transitions

```
┌─────────┐
│ CREATED │
└────┬────┘
     │
     ├─────────────────┐
     │                 │
     ▼                 ▼
┌──────────┐    ┌──────────┐
│AUTHORIZED│    │ CAPTURED │ (via purchase)
└────┬─────┘    └────┬─────┘
     │                │
     │                │
     ▼                ▼
┌──────────┐    ┌──────────┐
│ CAPTURED │    │ REFUNDED │
└────┬─────┘    │ or       │
     │          │ PARTIALLY│
     │          │ REFUNDED │
     │          └──────────┘
     │
     ├──────────┐
     │          │
     ▼          ▼
┌──────────┐  ┌──────────┐
│CANCELLED │  │ REFUNDED │
└──────────┘  └──────────┘
```

**Valid State Transitions:**
- `CREATED` → `AUTHORIZED` (via authorize)
- `CREATED` → `CAPTURED` (via purchase)
- `AUTHORIZED` → `CAPTURED` (via capture)
- `AUTHORIZED` → `CANCELLED` (via cancel)
- `CAPTURED` → `CANCELLED` (via cancel)
- `CAPTURED` → `REFUNDED` (via full refund)
- `CAPTURED` → `PARTIALLY_REFUNDED` (via partial refund)
- `PARTIALLY_REFUNDED` → `REFUNDED` (via remaining refund)

**Invalid Transitions:**
- Cannot capture from CREATED (must authorize first)
- Cannot refund from AUTHORIZED (must capture first)
- Cannot cancel from REFUNDED
- Cannot capture from CANCELLED

### Transaction Status

- **SUCCESS**: Transaction completed successfully
- **FAILED**: Transaction failed (eligible for retry)
- **PENDING**: Transaction awaiting processing (for async operations)

## Business Rules

### Payment Validation Rules

1. **Amount Validation**
   - Amount must be greater than 0.01
   - Amount must not exceed maximum limit (if configured)

2. **Card Validation**
   - Card number: 13-19 digits
   - Expiration date: Valid future date in YYYY-MM format
   - CVV: 3-4 digits

3. **State Transition Rules**
   - Capture only allowed from AUTHORIZED status
   - Refund only allowed from CAPTURED status
   - Cancel allowed from AUTHORIZED or CAPTURED status
   - Cannot perform operations on CANCELLED or REFUNDED orders

4. **Idempotency**
   - Retry operations use unique retry IDs
   - Same retry ID cannot be processed twice
   - Maximum 3 retry attempts per transaction

### Retry Logic

1. **Eligible Transactions**
   - Status: FAILED
   - Retry attempts < 3

2. **Retry Process**
   - Generate unique retry ID
   - Check idempotency (retry ID not already processed)
   - Increment retry attempt counter
   - Re-execute payment gateway call
   - Update transaction status

## Security

### Authentication Flow

```
Client                    API                    Database
  │                        │                        │
  │──POST /login───────────▶│                        │
  │   (username, password)  │                        │
  │                        │                        │
  │                        │──Validate Credentials─▶│
  │                        │◀──User Found───────────│
  │                        │                        │
  │                        │──Generate JWT──────────│
  │                        │                        │
  │◀──200 OK───────────────│                        │
  │   (JWT token)           │                        │
  │                        │                        │
  │──API Request───────────▶│                        │
  │   (Authorization:       │                        │
  │    Bearer <token>)     │                        │
  │                        │                        │
  │                        │──Validate JWT──────────│
  │                        │                        │
  │◀──Response─────────────│                        │
```

### Security Features

1. **JWT Authentication**
   - Token-based authentication
   - Configurable expiration (default: 24 hours)
   - Secret key stored securely

2. **Password Encryption**
   - Passwords stored encrypted in database
   - BCrypt hashing

3. **API Security**
   - All payment endpoints require authentication
   - HTTPS recommended for production

## Error Handling

### Error Response Format

```json
{
  "error": "Error message",
  "status": 400,
  "timestamp": "2025-01-21T10:30:00"
}
```

### HTTP Status Codes

- `200 OK` - Successful operation
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request data or business rule violation
- `401 Unauthorized` - Missing or invalid authentication token
- `402 Payment Required` - Payment failed
- `404 Not Found` - Resource not found
- `502 Bad Gateway` - Payment gateway error

## Performance Considerations

1. **Database**
   - Indexed on `order_id` and `transaction_id` for fast lookups
   - Connection pooling enabled

2. **Transaction Management**
   - Database transactions for consistency
   - No rollback on PaymentException (to preserve transaction record)

3. **Retry Mechanism**
   - Asynchronous retry processing
   - Configurable retry attempts and delays

## Monitoring and Logging

- All payment operations are logged
- Transaction status tracked in database
- Authorize.Net responses logged for debugging
- Retry attempts tracked in transaction metadata

## Future Enhancements

1. **Async Processing**
   - Queue-based processing for high volume
   - Webhook support for payment status updates

2. **Enhanced Retry**
   - Exponential backoff
   - Configurable retry strategies per error type

3. **Analytics**
   - Payment success rate tracking
   - Transaction volume metrics
   - Error rate monitoring
