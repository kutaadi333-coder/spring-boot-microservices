# Day 2 — API Gateway Policies and Secure Requests

**Date:** 27-Oct-2026  
**Priority:** High

## 1. Objective

Configure API Gateway routing, authentication/authorization checks, rate limiting, request validation, and safe request handling across backend microservices.

## 2. Implementation

### API Gateway Routing
- Configured routes for User Service, Order Service, Product Service, and Payment Service.
- Used Eureka service discovery and load balancing to forward requests to backend services.
- Applied Gateway filters for correlation IDs, rate limiting, and request/response logging on configured routes.

### Security
- Configured HTTP Basic authentication for protected Gateway endpoints.
- Configured public access for health, information, public product, and authentication URL patterns.
- Supplied Gateway credentials through environment variables rather than hardcoding them in application configuration.
- Configured BCrypt password encoding for the in-memory Gateway user.

### Rate Limiting
- Integrated the Gateway rate-limit filter with Redis-compatible Memurai.
- Restored Memurai connectivity and confirmed `memurai-cli ping` returned `PONG`.

### Logging and Monitoring
- Added correlation IDs and request/response logging for configured Gateway routes.
- Configured Actuator exposure for health, info, metrics, and Prometheus.

## 3. Test Evidence

| Test | Observed result |
|---|---|
| Memurai connectivity | `PONG` |
| Public product endpoint through API Gateway (`GET /api/v1/products/public`) | `200 OK` |
| Protected product endpoint without authentication | `401 Unauthorized` |
| User Service user creation (`POST /api/v1/users`) | User created successfully |
| User Service validation with missing phone | `400 Bad Request` with `phone: Phone number cannot be blank` |

## 4. Key Decisions

- Gateway credentials are supplied through environment variables.
- BCrypt is used to encode the configured Gateway user's password.
- Public and protected endpoints use different access rules.
- Memurai provides the Redis-compatible service used by the configured rate-limit filter.
- Correlation IDs support request tracing across configured Gateway routes.

## 5. Outcome

Configured core API Gateway routing, security rules, rate-limit integration, and request logging. Confirmed Memurai connectivity, public endpoint access through the Gateway, rejection of unauthenticated protected requests, and User Service creation/validation behavior.
