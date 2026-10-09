# JIRA Task 05 --- Production API Review, Reliability Testing & System Design

**Date:** 09-Oct-2026\
**Effort:** 8 hours\
**Priority:** Critical\
**Epic:** EPIC 07 --- Advanced API Architecture, Observability &
Production System Design\
**Status:** Review in progress; results below reflect only observed
tests

## 1. Objective

Review the existing microservices platform as a production-style system.
Validate gateway behavior, rate limiting, idempotency, correlation-ID
logging, resilience mechanisms, and infrastructure dependencies.
Document observed results, unverified scenarios, risks, and follow-up
actions.

## 2. System Architecture

``` text
                         Client / Postman
                                |
                                v
                    API Gateway :8080
             Routing | Rate Limit | Correlation ID
             Request/Response Logging | Error Handling
                                |
             +------------------+------------------+
             |                  |                  |
             v                  v                  v
       User Service       Order Service      Product Service
          :8081               :8082               :8085
                                |
                    +-----------+-----------+
                    |                       |
                    v                       v
              Payment Service        Inventory Service
                  :8084                   :8083

      Supporting components:
      - Eureka Service Discovery :8761
      - Redis-compatible Memurai :6379 (rate limiting and idempotency)
      - Kafka broker :9092 (asynchronous messaging)
      - PostgreSQL databases for applicable services
      - H2 for Product Service
```

This is a high-level view of the known services and components. Exact
call paths and database ownership should be confirmed against current
source/configuration before using this as an authoritative deployment
diagram.

## 3. Review Results

### 3.1 API Gateway

  -----------------------------------------------------------------------------------
  Check                   Observed result                     Status
  ----------------------- ----------------------------------- -----------------------
  Route to Product        `GET /api/v1/products/public`       Passed
  Service                 returned `200 OK`                   

  Correlation ID response `X-Correlation-ID: TL-TASK05-001`   Passed
  header                  matched request                     

  Gateway request log     Included correlation ID, method,    Passed
                          URL, target service                 

  Gateway response log    Included correlation ID, service,   Passed
                          status, duration                    

  User Service            Gateway returned                    Passed for tested
  unavailable             `503 Service Unavailable` while     scenario
                          User Service was stopped            
  -----------------------------------------------------------------------------------

Example observed Gateway log:

``` text
Gateway Request | CorrelationId=TL-TASK05-001 | Method=GET | URL=http://localhost:8080/api/v1/products/public | Service=product-service
Gateway Response | CorrelationId=TL-TASK05-001 | Service=product-service | Status=200 | ResponseTime=1893ms
```

### 3.2 Rate Limiting

  -----------------------------------------------------------------------
  Check                   Observed result         Status
  ----------------------- ----------------------- -----------------------
  Normal request          Returned `200 OK`       Passed

  Excessive requests      PowerShell loop timed   Inconclusive
                          out / reported request  
                          failures; `429` was not 
                          confirmed               
  -----------------------------------------------------------------------

**Action required:** Re-test the configured rate limit with a controlled
client ID and confirm an actual `429 Too Many Requests` response.

### 3.3 Idempotency

Earlier JIRA Task 03 testing verified that repeated order requests with
the same `Idempotency-Key` returned the same order ID. Recorded examples
included `ORD-TEST-001` → order `10032`, `ORD-RETRY-001` → order
`10036`, and `ORD-RECORD-001` → order `10037`.

**Review result:** Previously verified; no new duplicate-order test was
performed during this task review.

### 3.4 Distributed Tracing and Structured Logging

A Gateway request using correlation ID `TL-TASK05-TRACE-001` reached the
Order Service and returned `200 OK`. The Order Service emitted:

``` text
Order Service Response | CorrelationId=TL-TASK05-TRACE-001 | Method=GET | Endpoint=/api/v1/orders/10037 | Status=200 | Duration=1545ms
```

The same log excerpt showed:

``` text
Circuit Breaker fallback triggered for userId=1
```

This confirms the fallback path was triggered during the request and the
Order Service still returned `200 OK`. The exact reason for the fallback
was not established by this excerpt alone.

Correlation-ID logging and response status/duration are verified for the
Gateway-to-Order-Service request. Full OpenTelemetry trace/span
integration has not been confirmed.

### 3.5 Infrastructure Availability

  -----------------------------------------------------------------------------------------------------------------------
  Component               Check                                                                   Result
  ----------------------- ----------------------------------------------------------------------- -----------------------
  Redis-compatible        `memurai-cli.exe ping`                                                  `PONG`
  Memurai                                                                                         

  Kafka                   `kafka-broker-api-versions.sh --bootstrap-server 192.168.177.99:9092`   Broker API versions
                                                                                                  returned successfully

  User Service            Stopped temporarily for Gateway failure test, then restarted            Restored

  Payment Service         Stopped temporarily; Postman request remained loading, then service     Failure test
                          restarted                                                               inconclusive; service
                                                                                                  restored
  -----------------------------------------------------------------------------------------------------------------------

Redis and Kafka were reachable when checked. This does not establish
application behavior if either dependency becomes unavailable.

## 4. Reliability Test Matrix

  -----------------------------------------------------------------------
  Scenario                Result                  Notes / next action
  ----------------------- ----------------------- -----------------------
  User Service            Passed                  Gateway returned `503`
  unavailable                                     

  Payment Service         Inconclusive            Request hung; no
  unavailable                                     reliable status
                                                  recorded

  Redis unavailable       Not tested              Test only if required;
                                                  restore promptly

  Kafka unavailable       Not tested              Broker reachability
                                                  verified while running

  Excessive API requests  Inconclusive            `429` not confirmed

  Duplicate order request Previously passed       Same idempotency key
                                                  returned same order ID
                                                  in Task 03

  Slow downstream service Not tested              No controlled latency
                                                  test performed

  Successful API response Passed                  `200 OK` observed

  Invalid order request   Previously observed     `400 Bad Request` for
                                                  invalid order ID

  Gateway                 Passed                  `503` observed for
  service-unavailable                             stopped User Service
  response                                        
  -----------------------------------------------------------------------

## 5. HTTP Response Code Review

  -----------------------------------------------------------------------
  Code                    Meaning                 Evidence in this review
  ----------------------- ----------------------- -----------------------
  `200`                   Successful request      Observed for Product
                                                  and Order requests

  `201`                   Resource created        Not verified in this
                                                  task review

  `400`                   Bad request             Previously observed for
                                                  invalid order ID

  `401`                   Unauthorized            Not verified

  `403`                   Forbidden               Not verified

  `404`                   Not found               Not verified as a
                                                  distinct response;
                                                  invalid order returned
                                                  `400` earlier

  `409`                   Conflict                Not verified

  `429`                   Rate limit exceeded     Not confirmed

  `500`                   Internal server error   Not verified in this
                                                  task review

  `503`                   Service unavailable     Observed when User
                                                  Service was stopped
  -----------------------------------------------------------------------

Do not manufacture status-code results. Mark a status as verified only
after observing it from the application.

## 6. Production-Readiness Checklist

Legend: **Verified** = observed in work to date; **Partial** = some
evidence exists but needs a targeted check; **Pending** = not confirmed.

-   [x] API Gateway routing and request/response logging checked
-   [ ] Authentication --- not verified in this review
-   [ ] Authorization --- not verified in this review
-   [ ] Rate limiting --- normal request passed; excessive-request `429`
    pending
-   [x] Idempotency --- verified previously in JIRA Task 03
-   [ ] Timeout configuration --- not comprehensively reviewed
-   [ ] Retry behavior --- not comprehensively reviewed
-   [x] Circuit breaker/fallback --- fallback log observed for User
    Service call
-   [ ] Kafka failure handling --- not tested with broker unavailable
-   [ ] Redis failure handling --- not tested with Redis unavailable
-   [ ] Database failure handling --- not tested
-   [x] Structured logging --- correlation ID, status, and duration
    observed
-   [ ] Full distributed tracing with OpenTelemetry --- not confirmed
-   [x] Error handling --- Gateway `503` observed for User Service
    unavailable
-   [ ] API documentation --- review existing Swagger/OpenAPI docs
    before marking complete

## 7. Single Points of Failure and Risk Review

  -----------------------------------------------------------------------------
  Component / dependency  Potential impact if     Mitigation / recovery to
                          unavailable             review
  ----------------------- ----------------------- -----------------------------
  API Gateway             External API entry      Multiple gateway instances,
                          point may be            health checks, deployment
                          unavailable             redundancy

  Individual service      Routes relying on that  Health checks, timeouts,
                          service fail or degrade circuit breakers, meaningful
                                                  error responses

  Redis / Memurai         Rate limiting and       Explicit failure policy,
                          idempotency may fail    monitoring, persistence/HA
                          depending on fallback   appropriate to requirements
                          behavior                

  Kafka                   Asynchronous events may Retry/DLQ strategy, consumer
                          fail to publish or be   monitoring, durable broker
                          consumed                setup

  PostgreSQL / H2         Database-dependent      Backups, health checks,
                          operations fail         recovery procedures,
                                                  production database HA where
                                                  required

  Eureka                  Service discovery may   Redundancy, health
                          be affected depending   monitoring, defined fallback
                          on client cache and     behavior
                          topology                

  Payment Service         Payment-related         Timeouts, circuit breaker,
                          operations may fail or  idempotent payment handling,
                          remain pending          compensation/reconciliation
  -----------------------------------------------------------------------------

These are design risks and suggested mitigations, not claims that these
protections are already implemented.

## 8. Troubleshooting Procedure

1.  Capture the request URL, timestamp, HTTP status, and
    `X-Correlation-ID`.
2.  Search Gateway request and response logs for that ID.
3.  Search destination-service logs for the same ID.
4.  Compare status and duration; inspect exception logs for the same
    request.
5.  Check the health of the downstream service and its dependencies.
6.  Record whether the test passed, failed, or was inconclusive.
7.  Restore any service stopped for a test and confirm it is healthy
    before continuing.

## 9. Remaining Work / Deliverables

-   [x] High-level architecture draft
-   [ ] Sequence diagrams for normal request, duplicate order, and
    service failure
-   [ ] Complete reliability test report after remaining required tests
-   [x] Failure scenarios recorded with accurate test status
-   [x] Production-readiness checklist draft
-   [ ] Export/verify Postman collection (current Postman interface
    prompted for sign-in when creating a collection)
-   [ ] Update project README with architecture and how to run/test
    services
-   [ ] Final technical presentation
-   [ ] Re-test rate limiting and capture `429`
-   [ ] Decide whether Redis/Kafka/downstream failure tests are
    mandatory and perform controlled tests if required

## 10. Summary

The review verified API Gateway routing, correlation-ID propagation,
structured Gateway and Order Service logs, a successful request, and a
`503` response when User Service was unavailable. Prior idempotency
testing is available from JIRA Task 03. Memurai returned `PONG`, and
Kafka broker API versions were returned when the broker was running.

The rate-limit `429` test and Payment Service failure test were
inconclusive. Redis-unavailable, Kafka-unavailable, and slow-downstream
scenarios have not been tested. The task should remain in progress until
required remaining deliverables and tests are completed.

## Suggested commit

``` bash
git add docs/JIRA-TASK-05-PRODUCTION-API-REVIEW.md
git commit -m "Document JIRA Task 05 production API review"
git push
```
