# Day 5 --- Resilience Capstone and Failure Testing

**Project:** Spring Boot Microservices\
**Date:** 28 October 2026\
**Status:** Completed

## 1. Objective

Verify the integrated microservices flow through the API Gateway,
capture successful and rejected request behavior, validate rate
limiting, and record partial-failure and recovery observations.

## 2. Environment

  ------------------------------------------------------------------------
  Component                                     Port Role
  --------------------- ---------------------------- ---------------------
  API Gateway                                   8080 Entry point for API
                                                     requests, request
                                                     filtering,
                                                     correlation IDs, rate
                                                     limiting, and
                                                     operational metrics

  User Service                                  8081 Provides user details

  Order Service                                 8082 Handles order
                                                     operations and
                                                     retrieves related
                                                     user details

  Product Service                               8085 Provides product API
                                                     endpoints

  Eureka                                        8761 Service discovery

  Redis/Memurai                                 6379 Stores rate-limit
                                                     counters and window
                                                     expiry
  ------------------------------------------------------------------------

## 3. Verification Results

  ----------------------------------------------------------------------------------------------------
  Check                   Request or method                                    Observed result
  ----------------------- ---------------------------------------------------- -----------------------
  Gateway health          `GET http://localhost:8080/actuator/health`          `200 OK`; status `UP`

  Public Product API      `GET http://localhost:8080/api/v1/products/public`   `200 OK`
  through Gateway                                                              

  Authentication boundary `GET http://localhost:8080/api/v1/products` without  `401 Unauthorized` with
                          required authorization                               a safe error response

  User Service direct     `GET http://localhost:8081/api/v1/users/1`           `200 OK` with user
  check                                                                        details

  Orders by user through  `GET http://localhost:8080/api/v1/orders/user/1`     Order data and user
  Gateway                                                                      details returned
                                                                               successfully

  Partial-failure         Earlier orders response contained `user: null` and   The order response
  observation             `User service temporarily unavailable` for one       still contained order
                          record                                               data

  Recovery verification   Repeated the User Service check and the              User details were
                          orders-by-user request                               returned; the
                                                                               temporary-unavailable
                                                                               message no longer
                                                                               appeared

  Rate-limit threshold    PowerShell loop using a consistent `X-User-ID` value 100 successful
                                                                               `HTTP 200` responses,
                                                                               followed by `HTTP 429`
  ----------------------------------------------------------------------------------------------------

## 4. Rate-Limit Test

The gateway rate limiter is configured for **100 requests per client in
a 60-second window**.

The test sent repeated requests to:

`http://localhost:8080/api/v1/products/public`

Every request used the same client identifier through the `X-User-ID`
header. The observed output reported:

-   **100 successful HTTP 200 responses**
-   **HTTP 429 Too Many Requests** after the allowed request count was
    exceeded

This verifies that the configured request limit is enforced. The filter
also returns a `Retry-After: 60` header when rejecting an over-limit
request.

## 5. Failure and Recovery Observations

An earlier orders response included order data while one record showed
`user: null` and the message `User service temporarily unavailable`. A
direct request to User Service subsequently returned `200 OK` with user
details. Repeating the orders-by-user request through the Gateway then
returned user details without that temporary-unavailable message.

This records the observed partial-failure response and successful
recovery check without assigning an unconfirmed root cause to the
earlier behavior.

## 6. Observability and Diagnostics

The API Gateway was verified to return a correlation ID in the response.
Gateway logs captured request and response details, including the
correlation ID, HTTP method, endpoint, downstream service, response
status, and duration.

The Prometheus actuator endpoint was also checked:

`GET http://localhost:8080/actuator/prometheus`

The endpoint returned `200 OK` and exposed HTTP request metrics,
including request counts for successful and unauthorized responses.

## 7. Commands and Requests Used

### PowerShell --- Gateway health

``` powershell
curl.exe -i http://localhost:8080/actuator/health
```

### Postman --- public Product API through Gateway

``` http
GET http://localhost:8080/api/v1/products/public
```

### Postman --- direct User Service check

``` http
GET http://localhost:8081/api/v1/users/1
```

### Postman --- orders by user through Gateway

``` http
GET http://localhost:8080/api/v1/orders/user/1
```

### Postman --- Prometheus metrics

``` http
GET http://localhost:8080/actuator/prometheus
```

## 8. Outcome

The capstone verification demonstrated successful requests through the
Gateway, authentication-boundary rejection, correlation-ID and metrics
visibility, partial-failure response behavior with a subsequent
successful recovery check, and enforcement of the configured rate limit
with an observed HTTP 429 response.
