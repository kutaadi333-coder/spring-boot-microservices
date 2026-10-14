# JIRA Task 03 — API Resilience & Idempotency

**Date:** 14-Oct-2026  
**Priority:** High  
**Status:** Core scenarios verified; additional resilience tests pending

## 1. Objective

Improve Order Service reliability using timeouts, bounded retries, circuit-breaker fallback, and idempotency. Validate normal order creation, duplicate requests, downstream service failure, and recovery.

## 2. Architecture and Dependencies

The Order Service depends on:

- **User Service:** Retrieves and validates user information through OpenFeign.
- **Eureka Discovery:** Helps locate registered User Service instances.
- **Kafka:** Publishes order events, including to the `order-created` topic.
- **Database:** Persists order records.
- **Idempotency mechanism:** Uses the `Idempotency-Key` request header to handle repeated requests.

### Request flow

1. The client sends `POST /api/v1/orders` with an idempotency key and order details.
2. The Order Service retrieves user information from User Service.
3. OpenFeign applies the configured timeout and retry settings.
4. If the downstream call fails, the circuit-breaker fallback is invoked.
5. The Order Service validates and processes the order.
6. The order is persisted and the configured order-event publishing workflow executes.
7. A response is returned to the client.

## 3. Resilience Configuration

### 3.1 OpenFeign timeout and retry settings

File: `UserServiceFeignConfig.java`

| Configuration | Value | Purpose |
|---|---:|---|
| Connection timeout | 2 seconds | Limits connection establishment time |
| Read timeout | 3 seconds | Limits waiting for the response |
| Initial retry interval | 200 ms | Initial retry backoff |
| Maximum retry interval | 1 second | Caps the retry interval |
| Maximum attempts | 3 | Includes the initial attempt |

The retry configuration uses `Retryer.Default` with bounded backoff.

**Note:** The configuration is verified, but the exact retry count during a transient failure has not yet been independently tested. Actual request duration can also depend on service discovery, fallback execution, and other processing.

### 3.2 Circuit breaker

The Order Service configuration includes:

| Setting | Value |
|---|---|
| Sliding window type | Count-based |
| Sliding window size | 10 calls |
| Minimum number of calls | 5 |
| Failure-rate threshold | 50% |
| Open-state wait duration | 10 seconds |
| Permitted calls in half-open state | 2 |

The fallback invocation was observed in the Order Service logs during the User Service outage. Exact open, half-open, and closed state transitions have not yet been independently verified.

## 4. Idempotency

The Order API accepts the `Idempotency-Key` request header.

Example: `Idempotency-Key: task03-test-003`

### Verified result

- Original order ID: `10040`
- Repeated request returned order ID: `10040`
- Database query returned one row for order ID `10040`.

SQL used:

```sql
SELECT id, user_id, status, amount
FROM orders
WHERE id = 10040;
```

The database verification confirmed one row for the tested order ID. This demonstrates successful duplicate-request handling for this scenario.

Concurrent duplicate requests, key expiration, reuse of a key with a different request body, and failure recovery involving stored idempotency records have not yet been tested.

## 5. Test Results

### Test 1 — Normal order creation

**Scenario:** User Service and Kafka were running.  
**Result:** Passed. The Order Service created an order and returned HTTP `201 Created`.

### Test 2 — Duplicate request

**Scenario:** Submit the same order request twice with `Idempotency-Key: task03-test-003`.  
**Result:** Passed for the tested scenario. Both requests returned order ID `10040`, and the database query returned one row for that ID.

### Test 3 — User Service unavailable

**Scenario:** Stop User Service and submit a request using `Idempotency-Key: task03-user-down-001`.

Observed logs:

```text
No servers available for service: user-service
Load balancer does not contain an instance for the service user-service
Circuit Breaker fallback triggered for userId=1
Validation failed: User does not exist in user-service with id: 1
```

**HTTP result:** `400 Bad Request`  
**Result:** Fallback invocation confirmed. The application's validation logic rejected the request because user information could not be retrieved. The HTTP 400 response reflects the current application error-handling behavior.

### Test 4 — Recovery after User Service restart

**Scenario:** Restart User Service and submit a new request using `Idempotency-Key: task03-recovery-001`.

- HTTP status: `201 Created`
- Order ID: `10041`

**Result:** Passed. The Order Service processed a new order after User Service was restored.

### Test 5 — Timeout and retry configuration

**Scenario:** Review `UserServiceFeignConfig.java`.  
**Result:** Configuration verified. Connection timeout is 2 seconds, read timeout is 3 seconds, and maximum attempts are configured as 3. A dedicated transient-failure test remains pending.

## 6. Summary of Evidence

| Area | Status | Evidence |
|---|---|---|
| Normal order creation | Passed | HTTP 201 |
| Duplicate request handling | Passed for tested scenario | Same order ID and one database row |
| Downstream failure detection | Passed | No User Service instance found |
| Fallback invocation | Passed | Fallback log observed |
| Recovery after restart | Passed | HTTP 201, order ID 10041 |
| Timeout/retry configuration | Verified | Configuration reviewed |
| Actual retry behavior | Pending | Transient-failure test required |
| Concurrent idempotency testing | Pending | Not performed |
| Circuit-breaker state transitions | Pending | Not independently verified |

## 7. Remaining Validation

Before claiming all resilience testing is complete, perform the following checks:

1. Test retry behavior under a controlled transient failure.
2. Verify circuit-breaker transitions through closed, open, and half-open states.
3. Test concurrent requests using the same idempotency key.
4. Confirm that reusing an idempotency key with a different payload is handled safely.
5. Verify timeout behavior with a deliberately slow downstream response.
6. Review idempotency behavior when database persistence or Kafka publishing fails.

These are additional validation items, not claims of completed implementation.

## 8. Conclusion

The Order Service has been tested for normal order creation, repeated requests with the same idempotency key, User Service unavailability, fallback invocation, and recovery after the downstream service restarts.

The configured timeout, retry, and circuit-breaker policies provide intended resilience controls. Additional fault-injection and concurrency tests are still required to validate their behavior comprehensively.

**JIRA Task 03 status:** Core scenarios verified; remaining resilience validation documented.
