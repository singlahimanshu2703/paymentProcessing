# API Test Script for Payment Processing System
# Make sure the application is running on http://localhost:8080

Write-Host "=== Payment Processing API Tests ===" -ForegroundColor Green
Write-Host ""

# Step 1: Login and get JWT token
Write-Host "1. Testing Login API..." -ForegroundColor Yellow
$loginBody = @{
    username = "admin"
    password = "password"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginBody
    
    $token = $loginResponse.token
    Write-Host "   ✓ Login successful! Token received." -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "   ✗ Login failed: $_" -ForegroundColor Red
    exit 1
}

# Step 2: Test Purchase API
Write-Host "2. Testing Purchase API..." -ForegroundColor Yellow
$purchaseBody = @{
    amount = 100.50
    cardNumber = "4111111111111111"
    expirationDate = "2025-12"
    cvv = "123"
    customerEmail = "test@example.com"
    description = "Test Purchase Order"
} | ConvertTo-Json

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

try {
    $purchaseResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/payments/purchase" `
        -Method POST `
        -Headers $headers `
        -Body $purchaseBody
    
    Write-Host "   ✓ Purchase successful!" -ForegroundColor Green
    Write-Host "   Order ID: $($purchaseResponse.orderId)" -ForegroundColor Cyan
    Write-Host "   Status: $($purchaseResponse.status)" -ForegroundColor Cyan
    Write-Host "   Amount: $($purchaseResponse.amount)" -ForegroundColor Cyan
    $orderId = $purchaseResponse.orderId
    Write-Host ""
} catch {
    Write-Host "   ✗ Purchase failed: $_" -ForegroundColor Red
    Write-Host "   Response: $($_.Exception.Response)" -ForegroundColor Red
}

# Step 3: Test Authorize API
Write-Host "3. Testing Authorize API..." -ForegroundColor Yellow
$authorizeBody = @{
    amount = 200.00
    cardNumber = "4111111111111111"
    expirationDate = "2025-12"
    cvv = "123"
    customerEmail = "test@example.com"
    description = "Test Authorize Order"
} | ConvertTo-Json

try {
    $authorizeResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/payments/authorize" `
        -Method POST `
        -Headers $headers `
        -Body $authorizeBody
    
    Write-Host "   ✓ Authorize successful!" -ForegroundColor Green
    Write-Host "   Order ID: $($authorizeResponse.orderId)" -ForegroundColor Cyan
    Write-Host "   Status: $($authorizeResponse.status)" -ForegroundColor Cyan
    $authOrderId = $authorizeResponse.orderId
    Write-Host ""
} catch {
    Write-Host "   ✗ Authorize failed: $_" -ForegroundColor Red
}

# Step 4: Test Capture API (if we have an authorized order)
if ($authOrderId) {
    Write-Host "4. Testing Capture API..." -ForegroundColor Yellow
    try {
        $captureResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/payments/capture/$authOrderId" `
            -Method POST `
            -Headers $headers
        
        Write-Host "   ✓ Capture successful!" -ForegroundColor Green
        Write-Host "   Status: $($captureResponse.status)" -ForegroundColor Cyan
        Write-Host ""
    } catch {
        Write-Host "   ✗ Capture failed: $_" -ForegroundColor Red
    }
}

# Step 5: Test Refund API (if we have a captured order)
if ($orderId) {
    Write-Host "5. Testing Refund API..." -ForegroundColor Yellow
    try {
        $refundResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/payments/refund/$orderId?amount=50.00" `
            -Method POST `
            -Headers $headers
        
        Write-Host "   ✓ Refund successful!" -ForegroundColor Green
        Write-Host "   Status: $($refundResponse.status)" -ForegroundColor Cyan
        Write-Host "   Refund Amount: $($refundResponse.amount)" -ForegroundColor Cyan
        Write-Host ""
    } catch {
        Write-Host "   ✗ Refund failed: $_" -ForegroundColor Red
    }
}

Write-Host "=== API Tests Complete ===" -ForegroundColor Green
