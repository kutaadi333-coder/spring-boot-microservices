# JIRA Task 04 --- Distributed Tracing, Correlation IDs & Structured Logging

**Epic:** EPIC 07 --- Advanced API Architecture, Observability &
Production System Design\
**Task:** JIRA Task 04\
**Date:** 08-Oct-2026\
**Effort:** 8 hours\
**Priority:** Critical\
**Status:** Implementation and basic request testing completed;
documentation prepared

## 1. Objective

Trace a request across microservices using a consistent correlation ID
and structured request/response logs. This helps identify where a
request failed and how long each service took to process it.

## 2. Key Concepts

-   **Correlation ID:** A unique identifier carried in the
    `X-Correlation-ID` HTTP header so logs from the same request can be
    associated.
-   **Distributed request:** A request that passes through more than one
    service, such as API Gateway to Order Service.
-   **Structured logging:** Logs that consistently record fields such as
    correlation ID, HTTP method, endpoint, status code, and duration.
-   **Trace and span:** In distributed tracing systems, a trace
    represents the overall request path and spans represent individual
    operations within that path. The current implementation uses
    correlation-ID logging; OpenTelemetry instrumentation has not been
    confirmed as implemented.

## 3. Implemented Behavior

### API Gateway

The Gateway correlation-ID filter reads `X-Correlation-ID` from an
incoming request or creates one when it is missing. The ID is forwarded
downstream and returned in the response header.

Gateway request/response logging includes the correlation ID, request
method/URL, routed service, response status, and duration.

### Order Service

The Order Service `CorrelationIdFilter` reads the incoming
`X-Correlation-ID`, creates a UUID if the header is missing, places the
value in the logging MDC, returns the ID in the response header, and
logs request and response details. The MDC value is removed after
request processing.

The response log records: - Correlation ID - HTTP method - Endpoint -
HTTP status - Duration in milliseconds

The Order Controller also logs the correlation ID for the request.

## 4. Request Flow

``` text
Client / Postman
      |
      | Request + X-Correlation-ID
      v
API Gateway (:8080)
  - Create/read correlation ID
  - Log request and response
      |
      | Forward same X-Correlation-ID
      v
Order Service (:8082)
  - Read correlation ID
  - Add it to MDC
  - Log request and response status/duration
      |
      v
Order database / downstream operations
```

The same ID allows Gateway and Order Service log entries for a request
to be correlated. This document only describes the path verified during
testing; it does not claim that every downstream service has been
instrumented.

## 5. Verification and Test Results

### Successful request

-   **Request:** `GET http://localhost:8080/api/v1/orders/10037`
-   **Request header:** `X-Correlation-ID: TL-TRACE-005`
-   **Observed result:** `200 OK`
-   **Response header:** `X-Correlation-ID: TL-TRACE-005`
-   **Observed Order Service response log:**

``` text
Order Service Response | CorrelationId=TL-TRACE-005 | Method=GET | Endpoint=/api/v1/orders/10037 | Status=200 | Duration=512ms
```

This confirms that the correlation ID reached the Order Service and that
the service logged the response status and processing duration.

### Invalid order ID

-   **Request:** GET an order ID that does not exist, using correlation
    ID `TL-TRACE-003`.
-   **Observed result:** `400 Bad Request`
-   **Observed:** The correlation ID appeared in the Order Service
    request log.

This test confirms that the ID was propagated for the tested failure
response. The exact application-level root cause should be taken from
the corresponding exception/error log; the status code alone does not
establish the root cause.

## 6. Troubleshooting Procedure

1.  Capture the `X-Correlation-ID` sent by the client or returned in the
    response.
2.  Search the API Gateway console logs for that exact ID.
3.  Search Order Service logs for the same ID.
4.  Compare request entries with response entries.
5.  Check the response status and duration to identify which service
    logged the failure or slow response.
6.  Inspect the exception/error log for the same ID to determine the
    root cause.
7.  If a service has no log entries for the ID, verify that the request
    reached that service and that its filter/logging configuration is
    active.

## 7. OpenTelemetry Note

OpenTelemetry is a standard approach for instrumenting applications to
produce distributed traces with trace and span context. The work
verified for this task uses correlation IDs and application logs.
OpenTelemetry SDK/exporter integration and a tracing backend have not
been confirmed as implemented, so they are not represented as completed
deliverables.

## 8. Completion Summary

**Verified in the current implementation** - API Gateway correlation-ID
generation/forwarding behavior. - Correlation ID propagation to Order
Service. - Correlation ID returned in the response header. - Gateway
request/response logging. - Order Service request/response logging with
status and duration. - Successful request test returning `200 OK`. -
Invalid order ID test returning `400 Bad Request`.

**Keep in mind** - The current verification covers the
Gateway-to-Order-Service path. - Do not claim end-to-end tracing across
Payment/Notification or OpenTelemetry tracing unless those paths are
implemented and tested.

## 9. Suggested Commit

``` bash
git add docs/JIRA-TASK-04-DISTRIBUTED-TRACING.md
git commit -m "Document JIRA Task 04 distributed tracing"
git push
```
