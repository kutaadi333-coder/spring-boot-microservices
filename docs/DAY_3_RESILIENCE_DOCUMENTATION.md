# Day 3 — Resilience: Retries, Timeouts, Circuit Breakers, and Idempotency

**Date:** 27 October 2026  
**Service:** Order Service (`localhost:8082`)

## 1. Objective

Improve Order Service reliability when it communicates with User Service and when clients retry order-creation requests. This work covers timeouts, retries, circuit-breaker fallback behavior, and idempotency.

## 2. Services and Dependencies

| Component | Address / Port | Purpose |
|---|---|---|
| Order Service | `http://localhost:8082` | Creates orders and calls User Service |
| User Service | `http://localhost:8081` | Provides user details |
| Eureka | `http://localhost:8761/eureka/` | Service discovery |
| Redis / Memurai | `localhost:6379` | Stores idempotency records |
| Kafka | `192.168.177.99:9092` | Publishes order events |

## 3. Resilience Configuration

### Feign client

`UserClient` calls the `user-service` discovered through Eureka, using base path `/api/v1/users` and operation `GET /{id}`. The configured fallback is `UserClientFallback`.

### Timeouts and retries

The Feign configuration uses:

```java
@Bean
public Request.Options requestOptions() {
    return new Request.Options(
        2, TimeUnit.SECONDS,
        3, TimeUnit.SECONDS,
        true
    );
}

@Bean
public Retryer retryer() {
    return new Retryer.Default(200, 1000, 3);
}
```

The YAML also configures `connectTimeout: 3000` and `readTimeout: 5000` for `user-service`, with circuit-breaker support enabled. The Java and YAML timeout values should be reviewed together to ensure the effective settings are intentional.

### Circuit breaker

```yaml
resilience4j:
  circuitbreaker:
    instances:
      UserClientgetUserByIdLong:
        failureRateThreshold: 50
        minimumNumberOfCalls: 5
        slidingWindowSize: 5
        slidingWindowType: COUNT_BASED
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 2
```

The configured threshold is 50%, the sliding window contains five calls, and the open-state wait duration is 10 seconds.

### Fallback

When the User Service call fails, `UserClientFallback` logs a warning and returns `null`. Order Service checks the result and rejects order creation when the user cannot be retrieved. During the outage test, the API returned `400 BAD_REQUEST` with a validation message that user ID `1` could not be found in User Service.

## 4. Idempotency

Order creation accepts an `Idempotency-Key` header. The service checks for an existing record, reserves a new key, processes the order, and stores the response.

- A first request with a new key processes the order.
- Repeating the same request with the same key should return the stored response rather than create another order.
- A key marked `PROCESSING` is rejected as already being processed.
- A failed request may leave a key in `PROCESSING` until its expiration; recovery of such records remains a follow-up improvement.

## 5. Test Evidence

### Test 1 — Infrastructure prerequisites

- Memurai/Redis `PING` returned `PONG`.
- Kafka broker connectivity was verified at `192.168.177.99:9092`.
- Order Service started successfully.

### Test 2 — Successful order creation

Request:

```http
POST http://localhost:8082/api/v1/orders
Content-Type: application/json
Idempotency-Key: day3-test-005
```

```json
{
  "userId": 1,
  "productId": 101,
  "productName": "Day 3 Resilience Test",
  "quantity": 1,
  "amount": 500.00
}
```

**Observed result:** `201 Created`.

### Test 3 — Idempotency replay

The same request body was sent again with `Idempotency-Key: day3-test-005`.

**Observed result:** The response returned the same order ID as the first request, confirming replay behavior for this test key.

### Test 4 — User Service outage and fallback

User Service was stopped temporarily. Order Service logs showed service-discovery/load-balancer warnings for `user-service` and the fallback log:

```text
Circuit Breaker fallback triggered for userId=1
```

The order request failed with `400 BAD_REQUEST`. One observed request took approximately 61 ms.

### Test 5 — Circuit breaker opened

The monitoring response reported:

```json
{
  "circuitBreakers": {
    "UserClientgetUserByIdLong": {
      "bufferedCalls": 5,
      "failedCalls": 3,
      "failureRate": "60.0%",
      "failureRateThreshold": "50.0%",
      "notPermittedCalls": 0,
      "slowCallRate": "0.0%",
      "slowCallRateThreshold": "100.0%",
      "slowCalls": 0,
      "slowFailedCalls": 0,
      "state": "OPEN"
    }
  }
}
```

**Conclusion:** The breaker reached `OPEN`: 3 of 5 recorded calls failed (60%), exceeding the configured 50% threshold.

### Test 6 — Request after User Service restart

After User Service was restarted, an order request succeeded.

- Order ID: `10047`
- User ID: `1` (Alex Mercer)
- Product: `Day 3 Recovery Test`
- Quantity: `1`
- Amount: `500.00`
- Status: `PENDING`

This proves order creation succeeded after User Service was restored. It does not alone prove the circuit breaker returned to `CLOSED`; that state should be captured separately if required.

## 6. Endpoints Used

| Purpose | Method | URL |
|---|---|---|
| Create order | `POST` | `http://localhost:8082/api/v1/orders` |
| Actuator root/health response observed | `GET` | `http://localhost:8082/actuator` |
| Circuit-breaker status, if exposed | `GET` | `http://localhost:8082/actuator/circuitbreakers` |
| User Service health, if Actuator is enabled | `GET` | `http://localhost:8081/actuator/health` |

The observed `GET /actuator` response was:

```json
{
  "groups": ["liveness", "readiness"],
  "status": "UP"
}
```

A later request to `GET /actuator/circuitbreakers` returned `405 Method Not Allowed`. The breaker-state JSON above was obtained earlier, but endpoint exposure and supported method should be checked before relying on that URL consistently.

## 7. Follow-up Items

1. Capture an explicit `HALF_OPEN` or `CLOSED` circuit-breaker state after recovery.
2. Confirm the correct Actuator/Resilience4j endpoint and supported HTTP method.
3. Consider safe cleanup/recovery for idempotency keys left in `PROCESSING` after failures.
4. Consider a typed fallback/error response instead of returning `null`.
5. Review Java and YAML timeout settings together.

## 8. Acceptance Evidence

- [x] Order Service started successfully.
- [x] Redis/Memurai ping returned `PONG`.
- [x] Kafka broker connectivity verified.
- [x] New order request returned `201 Created`.
- [x] Duplicate request with the same idempotency key returned the same order ID.
- [x] User Service outage triggered the fallback.
- [x] Circuit-breaker monitoring reported `OPEN` at a 60% failure rate against a 50% threshold.
- [x] Order creation succeeded after User Service restarted.
- [ ] Explicit circuit-breaker recovery state captured.

## 9. Summary

Day 3 demonstrated configured Feign timeouts/retries, fallback behavior during a User Service outage, a circuit breaker reaching `OPEN` after repeated failures, and idempotent replay of an order-creation request. Order creation also succeeded after User Service was restarted. Explicit breaker-recovery evidence and Actuator endpoint consistency remain follow-up items.
