# JIRA TASK 01 â€” API Gateway Routing, Filtering & Request Policies

**EPIC:** EPIC 07 â€“ Advanced API Architecture, Observability & Production System Design  
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
                    â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
                    â”‚ Client / Postmanâ”‚
                    â””â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                             â”‚
                             â–¼
                    â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
                    â”‚   API Gateway   â”‚
                    â”‚     :8080       â”‚
                    â”‚                 â”‚
                    â”‚ â€¢ Routing       â”‚
                    â”‚ â€¢ Correlation IDâ”‚
                    â”‚ â€¢ Logging       â”‚
                    â””â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                             â”‚
              â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
              â”‚              â”‚              â”‚
              â–¼              â–¼              â–¼
        â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”   â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”   â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
        â”‚   User   â”‚   â”‚  Order   â”‚   â”‚ Product  â”‚
        â”‚  :8081   â”‚   â”‚  :8082   â”‚   â”‚  :8085   â”‚
        â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜   â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜   â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                             â”‚
                             â–¼
                       â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
                       â”‚ Payment  â”‚
                       â”‚  :8084   â”‚
                       â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜

                    â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
                    â”‚     Eureka      â”‚
                    â”‚      :8761      â”‚
                    â”‚ Service Discoveryâ”‚
                    â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
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

**JIRA TASK 01 â€” API Gateway Routing, Filtering & Request Policies: COMPLETED**


## 12. Cross-Service API Contract Review

**Review Date:** 12-Oct-2026  
**Status:** Contract review completed; Gateway response validation pending.

### 12.1 User Service â€” Port 8081

**Base Path:** `/api/v1/users`

| Method | Endpoint | Success Response |
|---|---|---|
| POST | `/api/v1/users` | 201 Created |
| GET | `/api/v1/users/{id}` | 200 OK |
| GET | `/api/v1/users` | 200 OK |
| PUT | `/api/v1/users/{id}` | 200 OK |
| DELETE | `/api/v1/users/{id}` | 200 OK |

Create and update operations use request DTOs with validation enabled. Delete returns the message `User deleted successfully`.

### 12.2 Order Service â€” Port 8082

**Base Path:** `/api/v1/orders`

| Method | Endpoint | Success Response |
|---|---|---|
| POST | `/api/v1/orders` | 201 Created |
| GET | `/api/v1/orders/{id}` | 200 OK |
| GET | `/api/v1/orders/user/{userId}` | 200 OK |
| PUT | `/api/v1/orders/{id}/status` | 200 OK |
| DELETE | `/api/v1/orders/{id}` | 204 No Content |
| POST | `/api/v1/orders/{id}/compensate` | 200 OK |
| POST | `/api/v1/orders/{id}/saga` | 200 OK |

**Contract requirements:**
- Order creation requires the `Idempotency-Key` request header.
- Order creation validates the request using `CreateOrderRequest`.
- Status updates use the `status` query parameter.
- Compensation accepts an optional `reason` query parameter.
- Saga requests accept `simulateFailure`, defaulting to `false`.

### 12.3 Product Service â€” Port 8085

**Base Path:** `/api/v1/products`

| Method | Endpoint | Success Response |
|---|---|---|
| GET | `/api/v1/products/public` | 200 OK |
| POST | `/api/v1/products` | 200 OK |
| GET | `/api/v1/products` | 200 OK |
| GET | `/api/v1/products/{id}` | 200 OK |
| PUT | `/api/v1/products/{id}` | 200 OK |
| PATCH | `/api/v1/products/{id}` | 200 OK |
| DELETE | `/api/v1/products/{id}` | 204 No Content |

The product-list endpoint supports pagination, sorting, category filtering, minimum and maximum price filters, and keyword search.

Default query parameters are `page=0`, `size=10`, `sortBy=id`, and `sortDir=asc`.

The controller rejects negative page numbers, non-positive page sizes, negative prices, and minimum prices greater than maximum prices.

**Review note:** Product creation currently returns `200 OK`. Consider `201 Created` if the endpoint creates a new resource and that status is adopted as the API contract.

### 12.4 Payment Service â€” Port 8084

**Base Path:** `/api/v1/payments`

| Method | Endpoint | Success Response |
|---|---|---|
| GET | `/api/v1/payments` | 200 OK |
| GET | `/api/v1/payments/{id}` | 200 OK |

The current controller returns payment-service status information and a payment lookup response. These responses are constructed in the controller; actual payment processing and persistence are not demonstrated by this controller.

### 12.5 Gateway Response Validation Finding

**Initial finding:** On 12-Oct-2026, a Gateway request to `GET /api/v1/users/1` returned HTTP 200 with an empty response body, although the direct User Service request returned JSON.

**Corrective change:** Configured the Gateway imperative HTTP client to use the JDK implementation and configured connection and response timeouts.

**Retest result:** Ten consecutive requests through the Gateway returned HTTP 200. This confirms that repeated requests completed successfully in the retest.

**Remaining validation:** Confirm the response body and content type through the Gateway, and test the Order, Product, and Payment routes individually before marking cross-service response validation complete.
### 12.6 Review Summary

The controllers for User, Order, Product, and Payment services have been reviewed for their endpoint paths, HTTP methods, and success responses.

Request DTO schemas, service-specific error response formats, and complete Gateway acceptance testing remain separate verification items. Do not mark the overall API contract task complete until required tests have been performed and recorded.

Verified the Payment Service controller behavior and documented the observed API contracts. Continued preparing the environment for API validation through Eureka, the microservices, and API Gateway. The Gateway response-body issue remains under investigation.

### 12.7 Cross-Service Error Response Contract

The microservices use HTTP status codes and JSON responses to communicate request failures. The exact response format depends on each service's exception-handling implementation.

| Service | Error scenario | HTTP status | Error identifier |
|---|---|---:|---|
| User Service | Invalid request fields | 400 | `VALIDATION_ERROR` |
| User Service | User not found | 404 | `USER_NOT_FOUND` |
| User Service | Duplicate email | 409 | `DUPLICATE_EMAIL` |
| User Service | Invalid user request | 400 | `INVALID_USER` |
| Order Service | Runtime exception | 400 | `BAD_REQUEST` |
| Order Service | Request validation failure | 400 | `VALIDATION_ERROR` |
| Product Service | Product not found | 404 | `Not Found` |
| Product Service | Invalid argument | 400 | `Bad Request` |
| Payment Service | Global exception handling | Not yet verified | Not yet verified |

**Order Service error response fields:** `timestamp`, `status`, `error`, `message`, and `path`.

**Product Service error response fields:** `status`, `error`, `message`, and `timestamp`.

**User Service error response fields:** `timestamp`, `status`, `error`, `message`, `path`, and optional `validationErrors`.

**Review finding:** Error-response formats and exception mappings are not fully standardized across services. Further validation is required to confirm not-found handling, unexpected server errors, and API Gateway error responses.

Only mark an error scenario as tested after verifying its actual HTTP response and response body.

### 12.8 API Request and Success-Response Contract Summary

This section summarizes the reviewed endpoints and the request/response behavior identified from the service controllers.

| Service | Method | Endpoint | Request details | Expected success response |
|---|---|---|---|---|
| User | POST | `/api/v1/users` | `CreateUserRequest` JSON | 201 Created; `UserResponse` |
| User | GET | `/api/v1/users/{id}` | User ID in path | 200 OK; `UserResponse` |
| User | GET | `/api/v1/users` | No request body | 200 OK; list of users |
| User | PUT | `/api/v1/users/{id}` | `UpdateUserRequest` JSON | 200 OK; `UserResponse` |
| User | DELETE | `/api/v1/users/{id}` | User ID in path | 200 OK; deletion message |
| Order | POST | `/api/v1/orders` | `CreateOrderRequest` JSON and `Idempotency-Key` header | 201 Created; `OrderResponse` |
| Order | GET | `/api/v1/orders/{id}` | Order ID in path | 200 OK; `OrderResponse` |
| Order | GET | `/api/v1/orders/user/{userId}` | User ID in path | 200 OK; list of orders |
| Order | PUT | `/api/v1/orders/{id}/status` | `status` query parameter | 200 OK |
| Order | DELETE | `/api/v1/orders/{id}` | Order ID in path | 204 No Content |
| Order | POST | `/api/v1/orders/{id}/compensate` | Optional `reason` query parameter | 200 OK |
| Order | POST | `/api/v1/orders/{id}/saga` | Optional `simulateFailure` query parameter | 200 OK |
| Product | GET | `/api/v1/products/public` | No request body | 200 OK; public API message |
| Product | POST | `/api/v1/products` | `ProductRequestDTO` JSON | 200 OK; `ProductResponseDTO` |
| Product | GET | `/api/v1/products` | Pagination, sorting, and optional filters | 200 OK; `ProductPageResponse` |
| Product | GET | `/api/v1/products/{id}` | Product ID in path | 200 OK; `ProductResponseDTO` |
| Product | PUT | `/api/v1/products/{id}` | `ProductRequestDTO` JSON | 200 OK; `ProductResponseDTO` |
| Product | PATCH | `/api/v1/products/{id}` | `ProductRequestDTO` JSON | 200 OK; `ProductResponseDTO` |
| Product | DELETE | `/api/v1/products/{id}` | Product ID in path | 204 No Content |
| Payment | GET | `/api/v1/payments` | No request body | 200 OK; service status map |
| Payment | GET | `/api/v1/payments/{id}` | Payment ID in path | 200 OK; payment status map |

**Notes**

- The table records behavior from the reviewed controller code. End-to-end testing is still required to verify the actual responses.
- Payment endpoints currently return controller-defined maps; actual payment processing and database retrieval are not demonstrated by these controller methods.
- Product creation currently returns HTTP 200. Consider HTTP 201 Created for successful resource creation as a separate API-design improvement.
- Verify the exact response schemas and endpoint behavior against the current source code before final sign-off.
