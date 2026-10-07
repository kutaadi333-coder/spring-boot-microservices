# JIRA TASK 03 — Idempotent APIs & Duplicate Request Handling

## Task Details

- **EPIC:** EPIC 07 — Advanced API Architecture, Observability & Production System Design
- **JIRA Task:** Task 03 — Idempotent APIs & Duplicate Request Handling
- **Date:** 07-Oct-2026
- **Effort:** 8 Hours
- **Priority:** Critical
- **Service:** Order Service
- **API:** POST /api/v1/orders
- **Storage:** Redis

---

## 1. Objective

Implement API idempotency for the Order API to prevent duplicate business operations when clients retry the same request.

The same `Idempotency-Key` must result in the same order response instead of creating a new order.

---

## 2. What is API Idempotency?

Idempotency means that sending the same request multiple times produces the same business result as sending it once.

```text
Client
   |
   | POST /api/v1/orders
   | Idempotency-Key: ORD-123
   v
Order Service
   |
   | Create Order
   v
Order Database
```

If the client retries:

```text
Client
   |
   | POST /api/v1/orders
   | Idempotency-Key: ORD-123
   v
Order Service
   |
   | Key already exists
   |
   | Do NOT create another order
   v
Return original response
```

This protects the system from duplicate orders caused by network retries, client retries, or lost responses.

---

## 3. Selected API

The idempotency implementation was applied to:

```text
POST /api/v1/orders
```

Required header:

```text
Idempotency-Key: <unique-key>
```

Example:

```text
Idempotency-Key: ORD-RECORD-001
```

---

## 4. Technology Used

Redis was selected for idempotency storage because Redis is already used in the project for API rate limiting.

The Order Service connects to Redis using:

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
```

Redis key format:

```text
idempotency:order:<Idempotency-Key>
```

Example:

```text
idempotency:order:ORD-RECORD-001
```

---

## 5. Idempotency Record

Each idempotency request is stored as a Redis JSON record containing:

```text
Idempotency Key
User ID
Request
Status
Response
Created Time
```

Example:

```json
{
  "idempotencyKey": "ORD-RECORD-001",
  "userId": 1,
  "request": {
    "userId": 1,
    "productId": 101,
    "productName": "Laptop",
    "quantity": 2,
    "amount": 50000
  },
  "status": "COMPLETED",
  "response": {
    "id": 10037,
    "userId": 1,
    "productName": "Laptop",
    "quantity": 2,
    "amount": 50000,
    "status": "PENDING"
  },
  "createdAt": "2026-10-07T13:29:32.0755276"
}
```

---

## 6. Request Processing Flow

### First Request

```text
POST /api/v1/orders
        |
        v
Read Idempotency-Key
        |
        v
Check Redis
        |
        |-- Key does not exist
        |
        v
Reserve key using Redis
        |
        v
Validate User
        |
        v
Create Order
        |
        v
Create OrderItem
        |
        v
Publish OrderCreated Kafka event
        |
        v
Store COMPLETED response in Redis
        |
        v
Return Order Response
```

---

## 7. Duplicate Request Flow

```text
Same Request
     |
     v
Same Idempotency-Key
     |
     v
Check Redis
     |
     v
Key exists
     |
     v
Status = COMPLETED
     |
     v
Return stored response
     |
     X
No new order created
```

---

## 8. Concurrent Request Protection

Redis atomic key reservation is used with:

```text
setIfAbsent()
```

This ensures that when multiple requests arrive with the same Idempotency-Key, only one request can reserve the key.

The other request detects that the key is already being processed or completed and does not create another order.

---

## 9. Test Case 1 — First Request

### Request

```text
POST http://localhost:8082/api/v1/orders
```

Header:

```text
Idempotency-Key: ORD-TEST-001
```

Request:

```json
{
  "userId": 1,
  "productId": 101,
  "productName": "Laptop",
  "quantity": 2,
  "amount": 50000
}
```

### Result

Order created successfully.

Order ID:

```text
10032
```

---

## 10. Test Case 2 — Duplicate Request

The exact same request was sent again with:

```text
Idempotency-Key: ORD-TEST-001
```

### Result

The response returned the same:

```text
Order ID: 10032
```

No duplicate order was created.

### Status

```text
PASSED
```

---

## 11. Test Case 3 — Verify Only One Order

The user order API was checked after the duplicate request.

Order `10032` was present with:

```text
Product: Laptop
Quantity: 2
Amount: 50000
Status: PENDING
```

The idempotency test therefore did not create another order for the same key.

### Status

```text
PASSED
```

---

## 12. Test Case 4 — Concurrent Requests

A new idempotency key was used:

```text
ORD-CONCURRENT-003
```

Two requests were sent concurrently with the same Idempotency-Key.

### Result

Both requests returned the same Order ID.

Only one business operation was created.

### Status

```text
PASSED
```

---

## 13. Test Case 5 — Retry After Response Loss

A new idempotency key was used:

```text
ORD-RETRY-001
```

The first request created:

```text
Order ID: 10036
```

The request was then retried using the same Idempotency-Key.

### Result

The retry returned the same:

```text
Order ID: 10036
```

No second order was created.

### Status

```text
PASSED
```

---

## 14. Test Case 6 — Redis Record Verification

A new request was tested using:

```text
Idempotency-Key: ORD-RECORD-001
```

The first request created:

```text
Order ID: 10037
```

The same request was then retried and returned:

```text
Order ID: 10037
```

Redis was then checked using:

```powershell
& "C:\Program Files\Memurai\memurai-cli.exe" GET "idempotency:order:ORD-RECORD-001"
```

Redis returned a completed JSON record containing:

```text
idempotencyKey
userId
request
status
response
createdAt
```

The stored status was:

```text
COMPLETED
```

The stored response contained:

```text
Order ID: 10037
```

### Status

```text
PASSED
```

---

## 15. Idempotency Storage Design

Redis key:

```text
idempotency:order:<Idempotency-Key>
```

Expiration:

```text
24 Hours
```

Processing status:

```text
PROCESSING
```

Completed status:

```text
COMPLETED
```

Redis atomic reservation:

```text
setIfAbsent()
```

This provides duplicate-request protection and supports concurrent request handling.

---

## 16. Implementation Components

### IdempotencyService

Responsible for:

- Reading existing idempotency records
- Reserving a new idempotency key
- Storing request information
- Storing processing status
- Storing completed response
- Managing Redis expiration

### IdempotencyRecord

Stores:

- Idempotency Key
- User ID
- Request
- Status
- Response
- Created Time

### OrderService

Uses the idempotency service before creating the order.

The order is created only when the idempotency key can be successfully reserved.

---

## 17. End-to-End Design

```text
                 Client
                   |
                   | POST /api/v1/orders
                   | Idempotency-Key
                   v
             Order Controller
                   |
                   v
            Order Service
                   |
                   v
              Redis Check
                   |
          +--------+--------+
          |                 |
      Key Exists        Key Missing
          |                 |
          v                 v
   Return Existing      Reserve Key
      Response              |
                            v
                       Validate User
                            |
                            v
                       Create Order
                            |
                            v
                       Order Database
                            |
                            v
                      Publish Kafka Event
                            |
                            v
                    Store Response in Redis
                            |
                            v
                       Return Response
```

---

## 18. Benefits

The implementation provides:

- Duplicate order prevention
- Safe client retries
- Response reuse
- Concurrent request protection
- Redis-based fast lookup
- 24-hour idempotency record retention
- Reduced risk of duplicate business transactions

---

## 19. Test Summary

| Test | Result |
|---|---|
| First request | PASSED |
| Duplicate request | PASSED |
| Verify single order | PASSED |
| Concurrent requests | PASSED |
| Retry request | PASSED |
| Redis record verification | PASSED |

---

## 20. Final Outcome

JIRA Task 03 successfully implements idempotent order creation using Redis.

The `Idempotency-Key` prevents duplicate order creation during repeated, concurrent, and retry requests.

The Redis record stores the required request and response information and allows the original order response to be returned for repeated requests using the same idempotency key.
