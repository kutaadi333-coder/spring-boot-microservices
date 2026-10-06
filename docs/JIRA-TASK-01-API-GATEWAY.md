# JIRA TASK 01 — API Gateway Routing, Filtering & Request Policies

**EPIC:** EPIC 07 – Advanced API Architecture, Observability & Production System Design  
**JIRA Task:** 01  
**Date:** 05-Oct-2026  
**Effort:** 8 Hours  
**Priority:** High  
**Status:** Completed

## 1. Objective

Implement and validate a centralized API Gateway for routing client requests to the appropriate microservices using Eureka service discovery, with correlation ID propagation and gateway request/response logging.

## 2. API Gateway

A Spring Cloud Gateway Server WebMVC application was used as the centralized entry point.

**Gateway Port:** 8080

The Gateway provides:
- Centralized routing
- Eureka-based service discovery
- Correlation/request ID handling
- Request and response logging
- Service availability testing

## 3. Configured Routes

| Gateway Endpoint | Target Service | Service Port |
|---|---|---:|
| `/api/v1/users/**` | User Service | 8081 |
| `/api/v1/orders/**` | Order Service | 8082 |
| `/api/v1/products/**` | Product Service | 8085 |
| `/api/v1/payments/**` | Payment Service | 8084 |

Routes use Spring Cloud LoadBalancer with Eureka service names.

## 4. Eureka Service Discovery

Eureka Server is running on port **8761**.

The API Gateway and backend services are registered with Eureka. The Gateway uses the registered service names to locate the target service instances.

## 5. Correlation ID

A `CorrelationIdFilter` was implemented in the API Gateway.

Behavior:
- Checks for an existing `X-Correlation-ID` request header.
- Generates a UUID when the header is not provided.
- Forwards the correlation ID to the downstream service.
- Returns the correlation ID in the Gateway response header.

## 6. Gateway Request/Response Logging

A `GatewayLoggingFilter` was implemented.

The Gateway logs:
- HTTP method
- Request URL
- Target service
- Response status
- Response time

Example:

```text
Gateway Request | Method=GET | URL=http://localhost:8080/api/v1/payments | Service=payment-service
Gateway Response | Service=payment-service | Status=200 | ResponseTime=32ms
```

## 7. Route Testing

### User Service

```text
GET http://localhost:8080/api/v1/users/1
```

Result: **Successful**

### Order Service

```text
GET http://localhost:8080/api/v1/orders/10024
```

Result: **Successful**

### Product Service

```text
GET http://localhost:8080/api/v1/products/public
```

Result:

```text
This is a public Product API
```

Result: **Successful**

### Payment Service

```text
GET http://localhost:8080/api/v1/payments
```

Result:

```json
{
  "status": "UP",
  "message": "Payment API is working",
  "service": "payment-service"
}
```

Result: **Successful**

## 8. Service Unavailable Scenario

The Payment Service was stopped while the API Gateway remained running.

Request:

```text
GET http://localhost:8080/api/v1/payments
```

The Gateway returned HTTP **500 Internal Server Error**, confirming the downstream service-unavailable scenario was detected.

## 9. Architecture

```text
                    ┌─────────────────┐
                    │ Client / Postman│
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │   API Gateway   │
                    │     :8080       │
                    │                 │
                    │ • Routing       │
                    │ • Correlation ID│
                    │ • Logging       │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
        ┌──────────┐   ┌──────────┐   ┌──────────┐
        │   User   │   │  Order   │   │ Product  │
        │  :8081   │   │  :8082   │   │  :8085   │
        └──────────┘   └──────────┘   └──────────┘
                             │
                             ▼
                       ┌──────────┐
                       │ Payment  │
                       │  :8084   │
                       └──────────┘

                    ┌─────────────────┐
                    │     Eureka      │
                    │      :8761      │
                    │ Service Discovery│
                    └─────────────────┘
```

## 10. Deliverables

- API Gateway application
- Eureka-based service discovery
- User, Order, Product and Payment routes
- Correlation ID generation and propagation
- Gateway request/response logging
- Response status and response-time logging
- Service unavailable scenario validation
- Architecture documentation

## 11. Final Status

**JIRA TASK 01 — API Gateway Routing, Filtering & Request Policies: COMPLETED**
