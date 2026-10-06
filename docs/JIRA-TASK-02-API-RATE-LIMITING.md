# JIRA TASK 02 — API Rate Limiting & Throttling

**Date:** 06-Oct-2026  
**Effort:** 8 Hours  
**Priority:** High  
**Status:** Completed

---

## 1. Objective

Protect APIs from excessive requests and traffic spikes by implementing Redis-based rate limiting at the API Gateway.

---

## 2. Rate Limiting Concepts

### Rate Limiting
Controls the number of API requests allowed from a user/client during a defined time period.

### Throttling
Restricts excessive API traffic to protect backend services.

### Request Quota
Defines the maximum number of requests allowed during the configured time window.

### Burst Traffic
Occurs when a large number of requests arrive within a short period.

### API Abuse
Occurs when APIs receive excessive or unauthorized traffic that can affect system availability and performance.

---

## 3. Protected APIs

The API Gateway rate limiter was applied to the following routes:

- User API
- Order API
- Product API
- Payment API

```text
/api/v1/users/**
/api/v1/orders/**
/api/v1/products/**
/api/v1/payments/**
```

---

## 4. Rate-Limit Rule

Final configuration:

```text
100 requests / minute / user
```

The user/client is identified using:

```text
X-User-ID: 1
```

Redis key format:

```text
rate-limit:<user-id>
```

Example:

```text
rate-limit:1
```

---

## 5. Architecture

```text
Client / Postman
       |
       v
API Gateway :8080
       |
       v
RateLimitFilter
       |
       v
Redis :6379
       |
       +----------------------+
       |                      |
 Limit Available        Limit Exceeded
       |                      |
       v                      v
Downstream Service        HTTP 429
       |
       v
User / Order / Product / Payment
```

---

## 6. Redis Configuration

Redis was configured in the API Gateway:

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
```

Redis was verified as available on port `6379`.

---

## 7. Rate-Limit Implementation

The Gateway uses `StringRedisTemplate` to maintain request counters.

Redis key:

```text
rate-limit:<user-id>
```

A 60-second expiration window is created for the first request.

Final configuration:

```text
MAX_REQUESTS = 100
WINDOW = 1 minute
```

---

## 8. HTTP 429 Handling

When the request count exceeds the configured limit, the Gateway returns:

```text
HTTP 429 Too Many Requests
```

Response header:

```text
Retry-After: 60
```

Response body:

```text
Too Many Requests
```

---

## 9. Normal Traffic Test

Endpoint:

```text
GET http://localhost:8080/api/v1/payments
```

Header:

```text
X-User-ID: 1
```

Result:

```text
HTTP 200 OK
```

Gateway logs confirmed:

```text
Rate Limit Redis Count | Client=1 | Count=1
Rate Limit Window Created | Client=1 | Window=60s
Rate Limit Allowed | Client=1 | Count=1
Gateway Response | Service=payment-service | Status=200 | ResponseTime=36ms
```

---

## 10. Excessive Traffic Test

For validation, the limit was temporarily reduced to `3 requests/minute`.

Test results:

```text
1st request → 200 OK
2nd request → 200 OK
3rd request → 200 OK
4th request → 429 Too Many Requests
```

This confirmed that requests above the configured threshold are blocked by the Gateway.

After the test, the configuration was restored to:

```text
100 requests / minute / user
```

---

## 11. Final Verification

Final configuration:

```text
MAX_REQUESTS = 100
WINDOW = 1 minute
```

A final Gateway request completed successfully with HTTP 200.

Gateway logs:

```text
Rate Limit Redis Count | Client=1 | Count=1
Rate Limit Window Created | Client=1 | Window=60s
Rate Limit Allowed | Client=1 | Count=1
Gateway Request | Method=GET | URL=http://localhost:8080/api/v1/payments | Service=payment-service
Gateway Response | Service=payment-service | Status=200 | ResponseTime=36ms
```

---

## 12. Deliverables

- Redis-based rate limiter — Completed
- API Gateway integration — Completed
- HTTP 429 handling — Completed
- Normal traffic test — Completed
- Excessive traffic test — Completed
- Rate-limit documentation — Completed
- Test evidence — Completed

---

## 13. Final Status

**JIRA TASK 02 — API Rate Limiting & Throttling: COMPLETED**

The API Gateway now uses Redis to control API request traffic and returns HTTP 429 when the configured request limit is exceeded.
