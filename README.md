# Payment Processing System

A Spring Boot application for processing payments through Authorize.Net integration. Supports purchase, authorize, capture, cancel, and refund operations with JWT-based authentication.

## Features

- **Payment Operations**: Purchase, Authorize, Capture, Cancel, and Refund
- **JWT Authentication**: Secure API access with token-based authentication
- **Transaction Management**: Complete transaction history and state tracking
- **Retry Mechanism**: Automatic retry for failed transactions with idempotency
- **Business Rules**: Enforced state transitions and validation
- **RESTful API**: Well-documented OpenAPI 3.0 specification

## Tech Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Security** - JWT authentication
- **Spring Data JPA** - Database persistence
- **MySQL** - Database (H2 for testing)
- **Authorize.Net SDK 3.0.0** - Payment gateway integration
- **Lombok** - Boilerplate reduction
- **Maven** - Build tool

## Prerequisites

- Java 17 or higher
- Maven 3.6+ (or use included Maven wrapper)
- MySQL 8.0+ (for production)
- Authorize.Net account (sandbox or production credentials)

## Configuration

### Application Properties

Create or update `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/paymentdb
    username: your_username
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

authorize:
  net:
    api-login-id: your_api_login_id
    transaction-key: your_transaction_key
    sandbox: true  # Set to false for production

jwt:
  secret: your-secret-key-min-256-bits
  expiration: 86400000  # 24 hours in milliseconds
```

### Environment Variables (Alternative)

You can also use environment variables:

```bash
export AUTHORIZE_NET_API_LOGIN_ID=your_api_login_id
export AUTHORIZE_NET_TRANSACTION_KEY=your_transaction_key
export AUTHORIZE_NET_SANDBOX=true
export JWT_SECRET=your-secret-key-min-256-bits
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/paymentdb
export SPRING_DATASOURCE_USERNAME=your_username
export SPRING_DATASOURCE_PASSWORD=your_password
```

## Database Setup

### MySQL

1. Create database:
```sql
CREATE DATABASE paymentdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. The application will automatically create tables on startup (with `ddl-auto: update`)

### Schema

The application creates the following tables:
- `users` - User accounts for authentication
- `orders` - Payment orders
- `transactions` - Payment transactions

See `src/main/resources/schema.sql` for the complete schema.

## Building the Project

### Using Maven Wrapper (Recommended)

```bash
# Windows
./mvnw.cmd clean install

# Linux/Mac
./mvnw clean install
```

### Using Maven (if installed)

```bash
mvn clean install
```

## Running the Application

### Development Mode

```bash
# Using Maven wrapper
./mvnw.cmd spring-boot:run

# Or using Maven
mvn spring-boot:run
```

### Production Mode

1. Build the JAR:
```bash
./mvnw.cmd clean package
```

2. Run the JAR:
```bash
java -jar target/processing-1.0.0.jar
```

The application will start on `http://localhost:8080`

## API Documentation

### Authentication

First, obtain a JWT token:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password"
  }'
```

Use the returned token in subsequent requests:

```bash
curl -X POST http://localhost:8080/api/payments/purchase \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "amount": 100.50,
    "cardNumber": "4111111111111111",
    "expirationDate": "2025-12",
    "cvv": "123",
    "customerEmail": "customer@example.com",
    "description": "Order #12345"
  }'
```

### API Endpoints

- `POST /api/auth/login` - Authenticate and get JWT token
- `POST /api/payments/purchase` - Process a purchase (authorize + capture)
- `POST /api/payments/authorize` - Authorize a payment
- `POST /api/payments/capture/{orderId}` - Capture an authorized payment
- `POST /api/payments/cancel/{orderId}` - Cancel a payment
- `POST /api/payments/refund/{orderId}` - Refund a captured payment

See `API-SPECIFICATION.yml` for complete API documentation with request/response schemas.

## Testing

### Run Unit Tests

```bash
./mvnw.cmd test
```

### Test Coverage

Test coverage reports are generated in `target/site/jacoco/index.html` after running tests.

### Test Data

A default test user is created on startup:
- Username: `admin`
- Password: `password`

**Note**: Change the default password in production!

## Project Structure

```
src/
├── main/
│   ├── java/com/payment/processing/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/     # REST controllers
│   │   ├── dto/            # Data transfer objects
│   │   ├── entity/         # JPA entities
│   │   ├── enums/          # Enumerations
│   │   ├── exception/      # Exception handlers
│   │   ├── repository/     # Data repositories
│   │   ├── security/       # Security/JWT components
│   │   └── service/        # Business logic
│   └── resources/
│       ├── application.yml # Application configuration
│       └── schema.sql      # Database schema
└── test/                   # Test classes
```

## State Transitions

### Order States
- `CREATED` → `AUTHORIZED` → `CAPTURED` → `REFUNDED`/`PARTIALLY_REFUNDED`
- `CREATED` → `CAPTURED` (for purchase)
- `AUTHORIZED`/`CAPTURED` → `CANCELLED`

### Transaction States
- `SUCCESS` - Transaction completed successfully
- `FAILED` - Transaction failed (eligible for retry)
- `PENDING` - Transaction awaiting processing

## Retry Mechanism

Failed transactions can be retried with idempotency protection:

```java
// Generate unique retry ID
String retryId = retryService.generateRetryId();

// Retry transaction
boolean success = retryService.retryTransaction(transactionId, retryId);
```

The retry service:
- Maximum 3 retry attempts
- Idempotency through unique retry IDs
- Tracks retry attempts in transaction metadata

## Troubleshooting

### Common Issues

1. **Database Connection Error**
   - Verify MySQL is running
   - Check database credentials in `application.yml`
   - Ensure database exists

2. **Authorize.Net API Errors**
   - Verify API credentials are correct
   - Check if sandbox mode matches your credentials
   - Review Authorize.Net response in logs

3. **JWT Token Expired**
   - Tokens expire after 24 hours (configurable)
   - Re-authenticate to get a new token

4. **Port Already in Use**
   - Change port in `application.yml`: `server.port: 8081`

## Production Considerations

1. **Security**
   - Use strong JWT secret (minimum 256 bits)
   - Enable HTTPS
   - Store credentials securely (use environment variables or secrets manager)
   - Change default admin password

2. **Database**
   - Use connection pooling
   - Enable database backups
   - Monitor query performance

3. **Monitoring**
   - Add application monitoring (e.g., Spring Boot Actuator)
   - Set up logging aggregation
   - Monitor payment gateway response times

4. **Error Handling**
   - Implement proper retry strategies
   - Set up alerting for payment failures
   - Log all transactions for audit

## License

This project is for demonstration purposes.

## Support

For issues or questions, please refer to the API specification (`API-SPECIFICATION.yml`) and architecture documentation (`ARCHITECTURE.md`).
