# Microservices Architecture — Production API Review

## 1. Project Overview

This project demonstrates a Spring Boot microservices architecture with API Gateway routing, service discovery, rate limiting, idempotent order handling, distributed correlation IDs, structured logging, and asynchronous messaging.

## 2. System Architecture

```text
                         Client / Postman
                                |
                                v
                    API Gateway :8080
              Routing | Rate Limiting | Logging
                  Correlation ID | Error Handling
                                |
          +---------------------+---------------------+
          |          |          |          |          |
          v          v          v          v          v
        User       Order      Inventory  Payment    Product
        :8081      :8082       :8083      :8084      :8085

Supporting Components:
- Eureka Service Discovery :8761
- Redis-compatible Memurai :6379
- Apache Kafka :9092
- PostgreSQL for applicable services
- H2 for Product Service
```

This is a high-level view of the project. Confirm actual service-to-service call paths and database ownership from the implementation before using this as a deployment specification.

## 3. Services and Ports

| Component | Port | Responsibility |
|---|---:|---|
| API Gateway | 8080 | Routing, request policies, correlation IDs, logging |
| User Service | 8081 | User-related APIs |
| Order Service | 8082 | Order APIs, idempotency, order processing |
| Inventory Service | 8083 | Inventory-related operations |
| Payment Service | 8084 | Payment-related operations |
| Product Service | 8085 | Product APIs |
| Eureka Server | 8761 | Service discovery |
| Memurai / Redis | 6379 | Rate limiting and idempotency support |
| Kafka | 9092 | Asynchronous messaging |

## 4. Implemented Features

- API Gateway routing to backend services.
- Correlation ID propagation using `X-Correlation-ID`.
- Gateway and Order Service request/response logging.
- Logging of HTTP status and request duration.
- Redis-based API rate limiting.
- Idempotent order creation using `Idempotency-Key`.
- Circuit breaker and fallback behavior.
- Kafka-based asynchronous messaging.
- Service discovery through Eureka.
- API pagination, filtering, sorting, and versioning in the Product Service.

## 5. Running the Project

1. Start the required infrastructure: databases, Memurai/Redis, Kafka, and Eureka.
2. Start the User, Order, Inventory, Payment, and Product services using their configured Spring Boot run configurations.
3. Start the API Gateway.
4. Confirm that each service starts successfully and registers with Eureka where configured.
5. Test API endpoints through the Gateway using Postman.

**Note:** Use each service's existing configuration and run instructions. Startup order, environment variables, database credentials, and profiles may vary by environment.

## 6. API Testing

Example successful Product API request:

```http
GET http://localhost:8080/api/v1/products/public
```

Example Order API request:

```http
GET http://localhost:8080/api/v1/orders/10037
X-Correlation-ID: TL-TASK05-TRACE-001
```

For order creation, include the required `Idempotency-Key` header and the request body expected by the Order Service.

## 7. Observability and Troubleshooting

The Gateway and Order Service use `X-Correlation-ID` to associate log entries for the same request.

When investigating a failure:

1. Record the request URL, HTTP status, and correlation ID.
2. Search Gateway logs for that correlation ID.
3. Search the target service logs for the same ID.
4. Compare response status and duration.
5. Review exception logs and downstream service health.

The current work verifies correlation-ID-based logging. Full OpenTelemetry tracing has not been confirmed.

## 8. Documentation

Project documentation is maintained in the `docs/` directory.

- `JIRA-TASK-04-DISTRIBUTED-TRACING.md`
- `JIRA-TASK-05-PRODUCTION-API-REVIEW.md`
- `JIRA-TASK-05-SEQUENCE-DIAGRAMS.md`

## 9. Production Readiness

Verified so far:
- Gateway routing and correlation-ID propagation.
- Structured Gateway and Order Service logs.
- Successful API request returning `200 OK`.
- Gateway response `503 Service Unavailable` when User Service was unavailable.
- Gateway response `503 Service Unavailable` when Payment Service was unavailable.
- Memurai connectivity (`PONG`).
- Kafka broker connectivity while Kafka was running.
- Idempotency tests completed in the earlier task.

Still requiring verification:
- Rate-limit response `429 Too Many Requests`.
- Redis-unavailable and Kafka-unavailable handling.
- Slow-downstream behavior.
- Remaining security and HTTP status-code checks.
- Final technical presentation and remaining JIRA Task 05 deliverables.

## 10. Technology Stack

- Java and Spring Boot
- Spring Cloud Gateway
- Eureka Service Discovery
- PostgreSQL and H2
- Redis-compatible Memurai
- Apache Kafka
- Postman
- Git
