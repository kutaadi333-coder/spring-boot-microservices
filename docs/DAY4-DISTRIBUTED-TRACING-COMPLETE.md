# Day 4 --- Distributed Tracing and Diagnostics

**Date:** 28 October 2026\
**Project:** Spring Boot API Gateway\
**Status:** Complete --- implementation, verification, and documentation
prepared

## 1. Objective

Verify correlation ID propagation, request/response logging, latency
logging, safe authentication errors, and Prometheus HTTP metrics for the
API Gateway.

## 2. Completed Implementation and Verification

### 2.1 Gateway Health Check

-   **Endpoint:** `GET http://localhost:8080/actuator/health`
-   **Result:** `200 OK`
-   **Observed status:** `UP`

### 2.2 Correlation ID Propagation

-   **Endpoint:** `GET http://localhost:8080/api/v1/products/public`
-   **Test correlation ID:** `day4-test-001`
-   **Result:** `200 OK`
-   Verified that the response included `X-Correlation-ID`.
-   The response initially contained the correlation ID header twice.
    The `CorrelationIdFilter.java` response handling was updated to use
    `headers.set(CORRELATION_ID_HEADER, finalCorrelationId)`.
-   Retested in Postman and confirmed that only one `X-Correlation-ID`
    response header is present.

### 2.3 Request and Response Logging

The Gateway console showed the following entries for the test request:

``` text
Gateway Request | CorrelationId=day4-test-001 | Method=GET | Path=/api/v1/products/public | Service=product-service
Gateway Response | CorrelationId=day4-test-001 | Service=product-service | Status=200 | DurationMs=432
```

The logs record the correlation ID, HTTP method, request path, target
service, response status, and request duration.

### 2.4 Negative Authentication Test

-   **Request:** `GET http://localhost:8080/api/v1/products`
-   **Authentication:** `No Auth` in Postman
-   **Result:** `401 Unauthorized`
-   Verified that the response body contains `errorId`, `status`, and
    `message`.
-   The error response uses a safe message and does not expose a stack
    trace.

### 2.5 Prometheus Metrics

-   **Endpoint:** `GET http://localhost:8080/actuator/prometheus`
-   **Result:** `200 OK`
-   **Observed response size:** approximately 40.01 KB in Postman.
-   Verified the presence of `http_server_requests_seconds_count`.
-   Observed metrics for HTTP status codes `200` and `401`, including
    `/actuator/health` and product API requests.
-   A Postman screenshot was captured during verification.

### 2.6 Automated Tests

-   **Command:** `mvn clean test`
-   **Result:** `BUILD SUCCESS`

## 3. Acceptance Evidence Summary

  --------------------------------------------------------------------------------------
  Requirement             Verification evidence                  Status
  ----------------------- -------------------------------------- -----------------------
  Gateway health          `/actuator/health` returned `200 OK`,  Passed
                          status `UP`                            

  Correlation ID          Supplied ID returned in response;      Passed
  propagation             duplicate header corrected and         
                          retested                               

  Request/response        Gateway console showed request and     Passed
  logging                 response log entries                   

  Latency logging         Sample response log recorded           Passed
                          `DurationMs=432`                       

  Safe negative response  No-auth request returned `401` with    Passed
                          `errorId`, `status`, and `message`     

  Prometheus metrics      `/actuator/prometheus` returned        Passed
                          `200 OK`                               

  HTTP request count      `http_server_requests_seconds_count`   Passed
  metrics                 showed `200` and `401` metrics         

  Automated tests         `mvn clean test` returned              Passed
                          `BUILD SUCCESS`                        
  --------------------------------------------------------------------------------------

## 4. Implementation Note

`CorrelationIdFilter.java` sets the response header using
`headers.set(CORRELATION_ID_HEADER, finalCorrelationId)` so this filter
replaces an existing value instead of appending another one. The
behavior was retested in Postman and the duplicate response header was
no longer present.

## 5. Completion Summary

Day 4 implementation and the documented verification checks are
complete. The Gateway health endpoint, correlation ID behavior,
request/response logging, latency logging, safe authentication
rejection, Prometheus metrics, and Maven test run were verified.

The Markdown file and captured screenshot can be copied into the main
Spring Boot microservices Git repository as project evidence. Repository
placement and Git commit are repository housekeeping steps, not
outstanding Day 4 implementation work.

## 6. Security Notes

-   Do not commit real passwords, access tokens, or other secrets.
-   Keep authentication error responses generic.
-   Avoid logging credentials or sensitive request data.
