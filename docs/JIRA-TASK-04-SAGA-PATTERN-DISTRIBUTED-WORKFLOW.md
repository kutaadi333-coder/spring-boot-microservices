# JIRA TASK 04 — Saga Pattern & Distributed Workflow

**Status:** COMPLETED  
**Date:** 30-Sep-2026 to 06-Oct-2026  
**Priority:** Critical

## Objective

Implement and validate the Saga pattern for coordinating a distributed business workflow across Order, Inventory, and Payment microservices using Kafka and compensating actions.

## 1. Saga Pattern Overview

The Saga pattern breaks a distributed business transaction into a sequence of local transactions.

Each service performs its own database operation and publishes an event for the next step.

If a later step fails, previously completed work is reversed through a compensating action.

```text
Local Transaction
       ↓
Kafka Event
       ↓
Next Service
       ↓
Local Transaction
       ↓
Kafka Event
```

## 2. Services Involved

| Service | Port | Responsibility |
|---|---:|---|
| Order Service | 8082 | Creates and tracks orders |
| Inventory Service | 8083 | Reserves and releases inventory |
| Payment Service | 8084 | Processes payment |
| Kafka | 9092 | Asynchronous event communication |

## 3. Successful Saga Workflow

The successful workflow implemented was:

```text
Create Order
     ↓
OrderCreated
     ↓
Kafka
     ↓
Reserve Inventory
     ↓
InventoryReserved
     ↓
Kafka
     ↓
Process Payment
     ↓
PaymentCompleted
     ↓
Kafka
     ↓
Order Confirmed
```

### Step-by-step

1. Client creates an order through Order Service.
2. Order Service saves the order with `PENDING` status.
3. Order Service publishes `OrderCreated`.
4. Inventory Service consumes the event.
5. Inventory Service reserves the requested quantity.
6. Inventory Service publishes `InventoryReserved`.
7. Payment Service consumes the inventory event.
8. Payment Service processes the payment.
9. Payment Service publishes `PaymentCompleted`.
10. Order Service consumes the payment event.
11. Order status is updated to `CONFIRMED`.

## 4. Failed Saga Workflow

The failure workflow was implemented for payment failure.

```text
Create Order
     ↓
Reserve Inventory
     ↓
Payment Failed
     ↓
Inventory Release Request
     ↓
Release Inventory
     ↓
InventoryReleased
     ↓
Order Cancelled
```

This provides compensation for work already completed by Inventory Service.

## 5. Kafka Events

The Saga workflow uses the following events:

```text
OrderCreated
InventoryReserved
PaymentCompleted
PaymentFailed
InventoryReleaseRequest
InventoryReleased
OrderCancelled
```

Kafka topics used:

```text
order-created
inventory-reserved
payment-events
inventory-release-request
inventory-released
order-compensation
order-saga
```

## 6. Order Status Flow

The Order Service tracks the Saga using order statuses.

### Successful flow

```text
PENDING
   ↓
INVENTORY_RESERVED
   ↓
PAYMENT_PROCESSING
   ↓
CONFIRMED
```

### Failure flow

```text
PENDING
   ↓
INVENTORY_RESERVED
   ↓
PAYMENT_FAILED
   ↓
CANCELLED
```

## 7. Compensation Workflow

When payment fails after inventory has already been reserved, the Order Service creates an inventory release request.

```text
PaymentFailed
     ↓
Order Service
     ↓
InventoryReleaseRequest
     ↓
Kafka
     ↓
Inventory Service
     ↓
Release Reserved Quantity
     ↓
InventoryReleased
     ↓
Kafka
     ↓
Order Service
     ↓
CANCELLED
```

The compensation action restores the reserved inventory back to available inventory.

## 8. End-to-End Failure Test — Order 10024

A fresh end-to-end failure scenario was executed using Order ID **10024**.

Test request:

```json
{
  "userId": 1,
  "productId": 101,
  "productName": "Laptop",
  "quantity": 1,
  "amount": 99999
}
```

The order was initially created successfully:

```text
Order ID: 10024
Status: PENDING
```

### Observed workflow

```text
Order 10024 Created
        ↓
Inventory Reserved
        ↓
Payment Failed
        ↓
Inventory Release Request Published
        ↓
Inventory Release Processed
        ↓
InventoryReleased Event Published
        ↓
Order Service Received Event
        ↓
Order 10024 → CANCELLED
```

## 9. Compensation Test Evidence

Order Service published the release request:

```text
Inventory release request published for Order ID: 10024
Inventory release requested for Order ID: 10024
```

Inventory Service processed the request:

```text
Inventory released successfully for Order ID: 10024
```

The inventory database update was executed:

```text
Hibernate:
    update
        inventory
    set
        available_quantity=?,
        product_id=?,
        product_name=?,
        reserved_quantity=?
    where
        id=?
```

Inventory Service then published:

```text
InventoryReleased event published for Order ID: 10024
```

Order Service received:

```text
Received Inventory Released Event for Order ID: 10024
```

The final order state was:

```text
Order 10024 cancelled after inventory compensation.
```

Final verified status:

```text
CANCELLED
```

## 10. Saga Architecture

```text
                         Client
                           |
                           ↓
                     Order Service
                         :8082
                           |
                    OrderCreated
                           |
                           ↓
                         Kafka
                           |
                           ↓
                 Inventory Service
                       :8083
                           |
                  InventoryReserved
                           |
                           ↓
                         Kafka
                           |
                           ↓
                  Payment Service
                       :8084
                      /                            /                             ↓           ↓
          PaymentCompleted   PaymentFailed
                    |           |
                    ↓           ↓
             Order Confirmed   Release Request
                                  |
                                  ↓
                          Inventory Service
                                  |
                           Release Inventory
                                  |
                           InventoryReleased
                                  |
                                  ↓
                            Order Service
                                  |
                                  ↓
                              CANCELLED
```

## 11. Kafka Consumer Groups

The implementation uses separate consumer groups for independent workflow responsibilities.

Important groups include:

```text
inventory-service-group
inventory-release-group
payment-service-group
order-payment-group
order-inventory-release-group
```

This allows each service to consume the events relevant to its responsibility.

## 12. Saga vs Local Transaction

### Local transaction

A local transaction protects operations inside one service and database.

```text
Order Service
     ↓
Order DB
```

### Saga

A Saga coordinates multiple local transactions across services.

```text
Order Service
     ↓
Kafka
     ↓
Inventory Service
     ↓
Kafka
     ↓
Payment Service
```

If a later operation fails, compensation is used instead of attempting one global database transaction.

## 13. Key Benefits Demonstrated

- Loose coupling between services.
- Asynchronous communication using Kafka.
- Independent database transactions.
- Failure handling through compensation.
- Event-driven workflow.
- Better resilience than a single distributed database transaction.
- Clear tracking of the business workflow through order status.

## 14. Testing Summary

| Test | Result |
|---|---|
| Order creation | Completed |
| OrderCreated Kafka event | Completed |
| Inventory reservation | Completed |
| InventoryReserved event | Completed |
| Payment success flow | Completed |
| PaymentCompleted event | Completed |
| Order confirmation | Completed |
| Payment failure flow | Completed |
| Inventory release request | Completed |
| Inventory compensation | Completed |
| InventoryReleased event | Completed |
| Order cancellation | Completed |
| End-to-end failure test — Order 10024 | Completed |

## 15. Final Result

The Saga workflow was successfully implemented across Order, Inventory, and Payment services using Kafka.

### Success

```text
Order
  ↓
Inventory
  ↓
Payment
  ↓
CONFIRMED
```

### Failure

```text
Order
  ↓
Inventory
  ↓
Payment FAILED
  ↓
Release Inventory
  ↓
CANCELLED
```

The end-to-end failure test using **Order 10024** confirmed that when payment fails, the inventory reservation is compensated and the order is ultimately changed to `CANCELLED`.

# Final Status

**JIRA TASK 04 — COMPLETED**

The Saga pattern and distributed workflow were successfully implemented and validated with both successful and failure/compensation scenarios.
