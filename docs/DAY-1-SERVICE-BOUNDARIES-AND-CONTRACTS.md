# Day 1 --- Service Boundaries and Contract Design

**Date:** 19 October 2026\
**Status:** In progress

## 1. Objective

Document the service boundaries, API contracts, security boundaries,
request flows, and error-handling expectations for the e-commerce
microservices architecture.

## 2. Service Boundaries

  ------------------------------------------------------------------------
  Component                                     Port Responsibility
  --------------------- ---------------------------- ---------------------
  API Gateway            8080 (verify configuration) Entry point and
                                                     request routing

  User Service                                  8081 User-related
                                                     operations

  Order Service                                 8082 Order management and
                                                     order workflows

  Product Service                               8085 Product CRUD, product
                                                     API, authentication
                                                     and JWT validation

  Payment Service        8084 (verify configuration) Payment processing

  Eureka Server                                 8761 Service discovery

  Kafka                                         9092 Asynchronous event
                                                     communication
  ------------------------------------------------------------------------

Ports and responsibilities should be verified against each service's
configuration before final sign-off.

## 3. Product Service API Contract

OpenAPI documentation: `http://localhost:8085/v3/api-docs`

Swagger UI: `http://localhost:8085/swagger-ui/index.html`

Documented endpoints include:

  Method   Endpoint                    Purpose
  -------- --------------------------- --------------------------------
  POST     `/api/v1/auth/register`     Register a user
  POST     `/api/v1/auth/login`        Authenticate and obtain tokens
  POST     `/api/v1/auth/refresh`      Refresh an access token
  GET      `/api/v1/products/public`   Public product listing
  GET      `/api/v1/products`          Retrieve products
  POST     `/api/v1/products`          Create a product
  GET      `/api/v1/products/{id}`     Retrieve a product by ID
  PUT      `/api/v1/products/{id}`     Update a product
  PATCH    `/api/v1/products/{id}`     Partially update a product
  DELETE   `/api/v1/products/{id}`     Delete a product
  GET      `/api/v2/products`          Version 2 product endpoint

## 4. Request Validation

The product request DTO defines these validation rules:

-   Product name must not be blank.
-   Product price is required and must be greater than zero.
-   Product category must not be blank.
-   Create and full-update operations should invoke bean validation
    using `@Valid`.

**Verification note:** Successful compilation was confirmed. Runtime
validation responses still need verification using an authorized
account.

## 5. Security and Trust Boundaries

-   Public product reads and authentication endpoints are configured as
    permitted routes.
-   Protected product operations require a valid access token.
-   Creating or updating products requires the `MANAGER` or `ADMIN`
    role.
-   Deleting products requires the `ADMIN` role.
-   Requests without a valid authenticated identity should be rejected.
-   An authenticated user lacking the required role should receive an
    access-denied response, normally HTTP 403.

## 6. Test Evidence

  Test                                      Observed result         Outcome
  ----------------------------------------- ----------------------- ---------
  `GET /api/v1/products/public`, no token   HTTP 200 OK             Passed
  `POST /api/v1/products`, no token         HTTP 401 Unauthorized   Passed
  Product Service compilation               `BUILD SUCCESS`         Passed
  `GET /v3/api-docs`                        HTTP 200 OK             Passed

## 7. Architecture Diagram

The following is a logical architecture view. Verify actual gateway
routes and service-to-service calls before sign-off.

``` mermaid
flowchart TD
    Client[Client / Postman]
    Gateway[API Gateway :8080]
    User[User Service :8081]
    Order[Order Service :8082]
    Product[Product Service :8085]
    Payment[Payment Service :8084]
    Eureka[Eureka :8761]
    Kafka[Kafka :9092]

    Client --> Gateway
    Gateway --> User
    Gateway --> Order
    Gateway --> Product
    Gateway --> Payment

    User -. Service discovery .-> Eureka
    Order -. Service discovery .-> Eureka
    Product -. Service discovery .-> Eureka
    Payment -. Service discovery .-> Eureka

    Order <--> Kafka
```

**Trust boundary:** The client-to-gateway boundary is the external entry
point. Authentication and authorization must also be enforced at the
appropriate service boundaries; gateway routing alone does not establish
authorization.

The diagram is a working architecture view and must be reconciled with
the actual gateway routes and inter-service calls before sign-off.

## 8. Decisions and Limitations

-   API contracts are documented through OpenAPI.
-   Request validation uses Jakarta Bean Validation.
-   JWT authentication and role-based authorization remain enabled.
-   Gateway routes, service dependencies, and the complete
    error-response schema require final verification.

## 9. Completion Checklist

-   [x] Product Service compiles successfully.
-   [x] OpenAPI endpoint responds successfully.
-   [x] Public endpoint success test captured.
-   [x] Unauthenticated protected endpoint rejection captured.
-   [x] Request DTO validation rules added.
-   [ ] Confirm consistent validation error responses.
-   [ ] Verify gateway routes and service dependencies.
-   [ ] Review and commit the intended Day 1 changes.
