# TASK 05 — Complete Resilient Microservices Workflow

**Date:** 25-Sep-2026  
**Effort:** 8 Hours  
**Priority:** Critical

## 1. Objective

Combine REST communication, timeout, retry, Circuit Breaker, fallback and Kafka into one complete microservices workflow.

## 2. Final Architecture

```text
                         Client
                           |
                           v
                     Order Service
                           |
                 +---------+---------+
                 |                   |
                 v                   v
          User Service            Kafka
                 |                   |
                 v                   v
             User DB        Notification Service
```

## 3. Complete Order Workflow

```text
Create Order
     |
     v
Validate Request
     |
     v
Save Order
     |
     v
Call User Service
     |
     v
Publish Order Created Event
```

The implemented Order Service validates the user through User Service, saves the order, and publishes an `ORDER_CREATED` event to Kafka.

## 4. REST Communication

Order Service communicates with User Service through the REST client/service-discovery setup.

Implemented endpoints used in the workflow include:

```text
GET /api/v1/users/{id}
GET /api/v1/orders/{id}
POST /api/v1/orders
```

## 5. Resilience Mechanisms

### Timeout

Order Service is configured with OpenFeign client timeouts for User Service:

- Connect timeout: 3000 ms
- Read timeout: 5000 ms

### Retry

Retry handling is part of the resilient inter-service communication workflow for transient User Service communication failures.

### Circuit Breaker

The User Service call is protected by a Resilience4j Circuit Breaker.

The circuit supports:

```text
CLOSED
   |
   | failures above threshold
   v
OPEN
   |
   | wait duration
   v
HALF_OPEN
   |
   +--> success --> CLOSED
   |
   +--> failure --> OPEN
```

Configured values include:

- Failure rate threshold: 50%
- Minimum number of calls: 5
- Sliding window size: 5
- Wait duration in open state: 10 seconds
- Permitted calls in half-open state: 2

### Fallback

When the User Service call is unavailable, the Order Service uses a controlled fallback response rather than allowing the downstream failure to propagate uncontrolled.

## 6. Kafka Communication

After an order is created, Order Service publishes an event to Kafka.

### Topic

```text
order-created
```

### Event

```json
{
  "orderId": 501,
  "userId": 101,
  "amount": 50000,
  "eventType": "ORDER_CREATED"
}
```

### Flow

```text
Order Service
      |
      v
Kafka topic: order-created
      |
      v
Notification Service
      |
      v
"Order created"
"User notification required"
```

## 7. Normal Flow

```text
Client
  |
  v
Order Service
  |
  +----> User Service
  |          |
  |          v
  |        User DB
  |
  +----> Kafka
             |
             v
     Notification Service
```

## 8. Normal Flow Verification

The normal flow was tested successfully.

An order was created successfully with:

```text
Order ID: 6
User ID: 1
Amount: 50000
Event Type: ORDER_CREATED
```

The Notification Service consumed the Kafka event and logged the order-created notification flow.

## 9. User Service Failure Flow

```text
Client
  |
  v
Order Service
  |
  v
User Service
  X
  |
  v
Timeout
  |
  v
Retry
  |
  v
Circuit Breaker
  |
  v
Fallback
```

This flow is supported by the implemented timeout, retry, Circuit Breaker and fallback configuration in Order Service.

## 10. Kafka / Notification Failure Flow

The failure-recovery scenario was explicitly tested.

### Test

```text
1. Stop Notification Service
2. Create Order
3. Order Service remains operational
4. Kafka receives the order-created event
5. Restart Notification Service
6. Notification Service consumes the pending event
```

### Actual test result

Order ID `7` was created while Notification Service was stopped.

Order response showed:

```text
id: 7
userId: 1
productName: Laptop
quantity: 2
amount: 50000
status: PENDING
```

After Notification Service was restarted, the consumer logged:

```text
Order created: orderId=7, userId=1, amount=50000, eventType=ORDER_CREATED
User notification require
```

This verified that the Kafka event remained available and was consumed after Notification Service recovery.

## 11. Sequence Diagram

```text
Client           Order Service       User Service       Kafka       Notification
  |                    |                  |               |               |
  |-- Create Order --->|                  |               |               |
  |                    |-- Validate ----->|               |               |
  |                    |<-- User OK ------|               |               |
  |                    |                  |               |               |
  |                    |-- Save Order ----|               |               |
  |                    |                  |               |               |
  |                    |-------------------------------> publish event    |
  |                    |                  |               |-------------->|
  |                    |                  |               |               |
  |<-- Order Response--|                  |               |               |
  |                    |                  |               |               |
```

## 12. Failure Sequence — User Service

```text
Client
  |
  v
Order Service
  |
  +--> User Service
        |
        X unavailable
        |
        v
     Timeout
        |
        v
       Retry
        |
        v
 Circuit Breaker
        |
        v
    Fallback
        |
        v
 Controlled response
```

## 13. Failure Sequence — Notification Service

```text
Client
  |
  v
Order Service
  |
  +--> Kafka --> event retained
                  |
                  X Notification Service stopped
                  |
                  v
             Notification Service restarted
                  |
                  v
             Event consumed
```

## 14. Logging Verification

The workflow produces logs for the major resilience and messaging stages:

- Request handling
- User Service communication
- Circuit Breaker fallback
- Kafka event publication
- Kafka consumer processing
- Notification processing

Example verified consumer log:

```text
Order created: orderId=7, userId=1, amount=50000, eventType=ORDER_CREATED
User notification require
```

## 15. Postman Collection Requirements

The final collection should include:

### User API

```text
GET http://localhost:8081/api/v1/users/1
```

### Create Order

```text
POST http://localhost:8082/api/v1/orders
```

Example request:

```json
{
  "userId": 1,
  "productName": "Laptop",
  "quantity": 2,
  "amount": 50000
}
```

### Get Order

```text
GET http://localhost:8082/api/v1/orders/{id}
```

### Failure Scenarios

Document/test:

- User Service unavailable
- Timeout
- Retry
- Circuit Breaker
- Fallback
- Notification Service unavailable
- Kafka event recovery

## 16. Git Submission

The implementation and Task 04 documentation are already present in the main repository:

```text
https://github.com/kutaadi333-coder/spring-boot-microservices
```

Task 04 documentation was pushed in commit:

```text
6f95077
Add Task 04 interservice communication resilience Kafka documentation
```

Task 05 final documentation and Postman collection are the remaining packaging items.

## 17. Final Demonstration

### Normal Flow

```text
Client
  |
  v
Order Service
  |
  v
User Service
  |
  v
Order Created
  |
  v
Kafka
  |
  v
Notification Service
```

### Failure Flow

```text
Client
  |
  v
Order Service
  |
  v
User Service
  X
  |
  v
Timeout
  |
  v
Retry
  |
  v
Circuit Breaker
  |
  v
Fallback
```

## 18. Task 05 Completion Status

| Requirement | Status |
|---|---|
| Complete Order workflow | Completed |
| REST communication | Completed |
| Timeout | Implemented |
| Retry | Implemented |
| Circuit Breaker | Implemented |
| Fallback | Implemented |
| Kafka Producer | Completed |
| Kafka Consumer | Completed |
| Normal flow verification | Completed |
| Kafka failure/recovery verification | Completed |
| Final architecture documentation | Completed in this document |
| Sequence diagram | Completed in this document |
| Postman collection | Pending |
| Final combined demonstration | Pending |
| Final Task 05 Git push | Pending |

## 19. Final Expected Output

A working Spring Boot microservices system where:

```text
Microservice Communication
          |
          v
         REST
          |
          v
      HTTP Client
          |
          v
       Timeout
          |
          v
        Retry
          |
          v
   Circuit Breaker
          |
          v
       Fallback
          |
          v
        Kafka
          |
          v
 Producer / Consumer
          |
          v
Event-Driven Architecture
```

The core resilient workflow is implemented. The remaining Task 05 work is the final Postman collection, final combined demonstration, and Git submission of those final deliverables.
