# JIRA Task 05 — Sequence Diagrams

## 1. Successful API Request

```mermaid
sequenceDiagram
    actor Client
    participant GW as API Gateway
    participant OS as Order Service
    participant DB as Order Database

    Client->>GW: GET /api/v1/orders/10037
    GW->>GW: Read correlation ID
    GW->>OS: Forward request and correlation ID
    OS->>OS: Log request
    OS->>DB: Retrieve order
    DB-->>OS: Return order data
    OS-->>GW: 200 OK + correlation ID
    GW-->>Client: 200 OK + correlation ID
```

## 2. User Service Unavailable

```mermaid
sequenceDiagram
    actor Client
    participant GW as API Gateway
    participant US as User Service

    Client->>GW: Request User API
    GW->>US: Forward request
    Note over US: Service unavailable
    US--xGW: Connection unavailable
    GW-->>Client: 503 Service Unavailable
    GW->>GW: Log status and response time
```

## 3. Payment Service Unavailable

```mermaid
sequenceDiagram
    actor Client
    participant GW as API Gateway
    participant PS as Payment Service

    Client->>GW: Payment-related request
    GW->>PS: Forward request
    Note over PS: Service stopped for test
    PS--xGW: Service unavailable
    GW-->>Client: 503 Service Unavailable
```

**Test note:** The User Service and Payment Service failure tests

## 4. Duplicate Request / Idempotency Flow

```mermaid
sequenceDiagram
    actor Client
    participant GW as API Gateway
    participant OS as Order Service
    participant Redis as Redis / Memurai
    participant DB as Order Database

    Client->>GW: POST /api/v1/orders + Idempotency-Key
    GW->>OS: Forward request and key
    OS->>Redis: Check idempotency key
    alt Key already processed
        Redis-->>OS: Return stored result reference
        OS-->>GW: Return existing order result
        GW-->>Client: Return existing order response
    else New key
        Redis-->>OS: Key not found
        OS->>DB: Create order
        DB-->>OS: Return created order
        OS->>Redis: Store key and result reference
        OS-->>GW: Return creation response
        GW-->>Client: Return creation response
    end
```

**Verification note:** This is a conceptual flow. Confirm the actual implementation, persistence behavior, and HTTP status codes against the Order Service before treating every step as verified.

## 5. Request Validation and Authorization

```mermaid
sequenceDiagram
    actor Client
    participant GW as API Gateway
    participant PS as Product Service
    participant JWT as JWT Authentication Filter
    participant SEC as Security Rules
    participant CTRL as Product Controller

    Client->>GW: Send product request
    GW->>GW: Apply gateway authentication
    GW->>PS: Forward request
    PS->>JWT: Read and validate Bearer token
    JWT-->>SEC: Authenticated principal and role
    SEC->>SEC: Check endpoint permission

    alt Token missing or invalid
        SEC-->>Client: 401 Unauthorized
    else Authenticated but insufficient role
        SEC-->>Client: 403 Forbidden
    else Authorized
        SEC->>CTRL: Continue request
        CTRL->>CTRL: Validate request body
        alt Invalid request body
            CTRL-->>Client: 400 Bad Request
        else Valid request
            CTRL-->>Client: Success response
        end
    end
```

**Verification note:** This diagram describes the intended security and validation decision points. Confirm actual response behavior and error formatting with integration tests.
