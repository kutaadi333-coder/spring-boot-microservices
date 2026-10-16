# JIRA Task 04 — Structured Logging, Correlation IDs & Metrics

**Date:** 16-Oct-2026  
**Priority:** High  
**Status:** Implementation and initial verification completed; final acceptance review pending.

## 1. Objective

Implement request correlation, request logging, HTTP response monitoring, and basic application metrics so API requests can be traced and failures and latency can be investigated.

## 2. Components

- **API Gateway:** Entry point for client requests.
- **Product Service:** Runs on port `8085`.
- **`CorrelationIdFilter`:** Accepts an incoming `X-Correlation-ID` header or generates a UUID when the header is missing or blank.
- **Mapped Diagnostic Context (MDC):** Makes the correlation ID available to the configured logging pattern.
- **Spring Boot Actuator and Micrometer:** Expose HTTP request measurements.

## 3. Correlation ID handling

The Product Service filter:
1. Reads `X-Correlation-ID` from the request.
2. Generates a UUID if the header is absent or blank.
3. Stores the ID in MDC.
4. Returns the ID in the response header.
5. Logs the HTTP method, URI, response status, and request duration.
6. Removes the MDC value after request processing.

Example request header:

```http
X-Correlation-ID: task04-final-001
```

Example observed log:

```text
2026-10-16T14:52:31.462+05:30 INFO [task04-final-001] [http-nio-8085-exec-2] c.e.p.filter.CorrelationIdFilter - Product Service Request | Method=GET | URI=/api/v1/products/public | Status=200 | DurationMs=62
```

## 4. Logging format

The Product Service console logging pattern includes timestamp, level, correlation ID, thread, logger, and message. The filter logs request method, URI, HTTP status, and duration in milliseconds.

Configured console pattern:

```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %-5level [%X{correlationId}] [%thread] %logger{36} - %msg%n"
```

## 5. Actuator metrics

The following endpoints were configured for local verification:

| Endpoint | Purpose |
|---|---|
| `/actuator/health` | Application health |
| `/actuator/info` | Application information |
| `/actuator/metrics` | Lists available metrics |
| `/actuator/metrics/http.server.requests` | HTTP server request measurements |

The `http.server.requests` metric exposes measurements such as:
- `COUNT`: number of recorded requests for the selected metric.
- `TOTAL_TIME`: cumulative request time in seconds.
- `MAX`: maximum recorded duration for the selected metric.
- Tags such as HTTP method, URI, status, outcome, and exception.

Observed metric snapshot during testing:
- Request count: `3`
- Status tags: `200`, `401`
- Outcome tags: `SUCCESS`, `CLIENT_ERROR`
- Total time: approximately `0.407` seconds

These are point-in-time test values, not permanent totals. One observed snapshot returned `MAX: 0.0`; validate this measurement further before using it for latency reporting.

## 6. Test evidence

Tests performed locally on **16-Oct-2026**:

| Scenario | Request | Observed result |
|---|---|---|
| Public product API success | `GET /api/v1/products/public` | `200 OK` |
| Protected endpoint without authentication | `GET /api/v1/products` | `401 Unauthorized` |
| HTTP request metrics | `GET /actuator/metrics/http.server.requests` | Metrics JSON returned |
| Correlation ID in Product Service | Header `task04-final-001` on public product request | Same ID visible in Product Service log |
| Status and duration logging | Public product request | `Status=200`, `DurationMs=62` observed |
| Gateway request | `GET http://localhost:8080/api/v1/products/public` | `200 OK` |
| Gateway-to-Product correlation | Header `task04-gateway-001` on gateway request | Same ID observed in Product Service log |

### Reproduction requests

Successful product request:

```http
GET http://localhost:8085/api/v1/products/public
X-Correlation-ID: task04-success-001
```

Unauthorized request (send without authentication):

```http
GET http://localhost:8085/api/v1/products
X-Correlation-ID: task04-failure-002
```

Metrics request:

```http
GET http://localhost:8085/actuator/metrics/http.server.requests
```

Gateway propagation request:

```http
GET http://localhost:8080/api/v1/products/public
X-Correlation-ID: task04-gateway-001
```

## 7. Security note

The local configuration permits unauthenticated access to selected Actuator endpoints for development testing. Protect these endpoints with appropriate authentication and network restrictions before production deployment.

## 8. Remaining acceptance checks

Before marking Task 04 fully complete, verify:

- [ ] The API Gateway itself logs correlation IDs, response status, and duration.
- [ ] Request counts, error rates, and latency are documented in a basic report or dashboard.
- [ ] Final evidence and documentation are reviewed against every JIRA acceptance criterion.

## 9. Conclusion

Local tests verified Product Service correlation ID handling, request status and duration logging, Actuator HTTP metrics, successful and unauthorized API requests, and correlation ID propagation from the API Gateway to Product Service. Complete the remaining acceptance checks above before closing Task 04.
