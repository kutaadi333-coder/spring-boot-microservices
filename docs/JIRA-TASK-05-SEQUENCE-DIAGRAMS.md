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