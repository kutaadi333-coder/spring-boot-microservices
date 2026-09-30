# JIRA TASK 03 — Distributed Transactions, Saga and Compensation

## 1. Objective

The objective of this task is to study and demonstrate transaction management in a microservices architecture.

The following concepts were studied and implemented:

- Local Transactions
- ACID properties
- Distributed Transaction Problem
- Two-Phase Commit (2PC)
- Eventual Consistency
- Compensation
- Saga Pattern
- Failure Scenarios
- Kafka-based event communication

The implementation was validated using the Order Service, PostgreSQL and Apache Kafka.

## 2. Local Transaction

A local transaction operates within a single service and its associated database. In the Order Service, Order and Order Item database operations are handled within a transaction using Spring's `@Transactional`.

```text
Create Order Request
        |
        v
Order Service
        |
        +----> Save Order
        |
        +----> Save Order Item
        |
        v
      Commit
```

Failure:

```text
Start Transaction
       |
       v
Save Order
       |
       v
Save Order Item
       |
       X
    Failure
       |
       v
Rollback
```

## 3. ACID Properties

### Atomicity
All operations in a transaction are completed successfully or none are committed.

### Consistency
The database moves from one valid state to another valid state.

### Isolation
Operations performed by one transaction are appropriately isolated from concurrent transactions.

### Durability
Once a transaction is committed, the committed data persists.

## 4. Rollback Demonstration

A rollback scenario was tested for the local transaction. The purpose was to verify that the database does not retain partially committed transaction data when the transaction fails.

## 5. Distributed Transaction Problem

In a microservices architecture, different services can have independent databases and transaction boundaries.

```text
Order Service
      |
      v
Payment Service
      |
      v
Inventory Service
```

If payment succeeds but inventory reservation fails, the system needs a mechanism to handle the already completed operation. This creates a distributed transaction consistency problem.

## 6. Two-Phase Commit (2PC)

Two-Phase Commit is a distributed transaction coordination approach.

### Phase 1 — Prepare

```text
Coordinator
     |
     +----> Service A: Prepare
     +----> Service B: Prepare
     +----> Service C: Prepare
```

### Phase 2 — Commit

```text
Coordinator
     |
     +----> Service A: Commit
     +----> Service B: Commit
     +----> Service C: Commit
```

If preparation fails, the transaction can be aborted. 2PC introduces coordination overhead and tighter coupling between participating resources.

## 7. Eventual Consistency

Eventual consistency means distributed components may not have exactly the same state immediately after an operation. The system reaches a consistent state after events and processing are completed.

```text
Order Created
      |
      v
Event Published
      |
      v
Consumer Processes Event
      |
      v
Order State Updated
```

## 8. Compensation

Compensation is a recovery mechanism used when a previously completed business operation needs to be logically reversed.

```text
Order Created
      |
      v
Payment Successful
      |
      v
Inventory Failed
      |
      v
Compensation
      |
      v
Cancel Order
```

In this project, Kafka is used to publish compensation events through the `order-compensation` topic. The `OrderCompensationConsumer` changes the order status to `CANCELLED`.

## 9. Saga Pattern

The Saga Pattern breaks a distributed business transaction into a sequence of smaller local transactions. If a later step fails, compensating actions are executed for previously completed steps.

```text
Order
  |
  v
Payment
  |
  v
Inventory
  |
  v
Completed
```

Failure:

```text
Order
  |
  v
Payment
  |
  X
Failure
  |
  v
Compensation
  |
  v
Cancel Order
```

## 10. Saga Implementation in Order Service

The Order Service uses Kafka for Saga event communication.

Main components:

```text
OrderSagaEvent
OrderSagaProducer
OrderSagaConsumer
OrderCompensationEvent
OrderCompensationProducer
OrderCompensationConsumer
```

Saga topic:

```text
order-saga
```

Compensation topic:

```text
order-compensation
```

## 11. Saga Success Flow

A successful Saga request publishes a `SAGA_SUCCESS` event.

```text
POST /api/v1/orders/{id}/saga
             |
             v
       SAGA_SUCCESS
             |
             v
       Kafka: order-saga
             |
             v
     OrderSagaConsumer
             |
             v
       Find Order
             |
             v
       Status = COMPLETED
             |
             v
       PostgreSQL Update
```

Tested Order ID: `10014`

Result:

```text
PENDING → SAGA_SUCCESS → COMPLETED
```

## 12. Saga Failure Flow

A failure can be simulated using:

```text
POST /api/v1/orders/{id}/saga?simulateFailure=true
```

Flow:

```text
POST Saga Request
        |
        v
   SAGA_FAILED
        |
        v
 Kafka: order-saga
        |
        v
OrderSagaConsumer
        |
        v
Compensation Event
        |
        v
Kafka: order-compensation
        |
        v
OrderCompensationConsumer
        |
        v
Status = CANCELLED
```

Tested Order ID: `10014`

Compensation reason:

```text
Saga test failure
```

Final status:

```text
CANCELLED
```

## 13. Comparison of Transaction Approaches

| Aspect | Local Transaction | Distributed Transaction | Saga |
|---|---|---|---|
| Scope | Single service/database | Multiple services/resources | Multiple local transactions |
| Transaction Boundary | Local database | Cross-resource | Per service |
| Coordination | Database transaction manager | Distributed coordinator | Events/commands |
| Consistency | Strong within transaction | Coordinated across participants | Eventual consistency |
| Coupling | Lower | Higher coordination dependency | Looser event-based coupling |
| Complexity | Lower | Higher | Medium/High |
| Failure Handling | Rollback | Commit/abort coordination | Compensation |
| Recovery | Database rollback | Distributed rollback/abort | Compensating actions |
| Example | Order + OrderItem | Order + Payment + Inventory | Order → Payment → Inventory |

## 14. Failure Scenarios

### Payment Failure

```text
Order Created
      |
      v
Payment Processing
      |
      X
Payment Failed
```

The order should not remain in a successful final state. Depending on business requirements it can be moved to a failure or cancellation state. In the implemented compensation flow, the final state used is `CANCELLED`.

Recovery:

```text
Payment Failed
      |
      v
Compensation Event
      |
      v
Order Compensation Consumer
      |
      v
Order = CANCELLED
```

Possible data requiring compensation includes order status, payment authorization/reservation, inventory reservation, and other completed business actions.

### Inventory Failure

```text
Order Created
      |
      v
Payment Successful
      |
      v
Inventory Reservation
      |
      X
Inventory Failed
```

A possible Saga recovery flow is:

```text
Inventory Failed
       |
       v
Compensation
       |
       +----> Cancel Order
       |
       +----> Reverse/Refund Payment
```

Exact compensation actions depend on business requirements.

## 15. Transaction Architecture

```text
                         Order API
                            |
                            v
                    +---------------+
                    | Order Service |
                    +-------+-------+
                            |
                     Kafka Events
                            |
             +--------------+--------------+
             |                             |
             v                             v
      +--------------+             +------------------+
      | Saga Consumer|             | Compensation     |
      |              |             | Consumer         |
      +------+-------+             +--------+---------+
             |                              |
             +--------------+---------------+
                            |
                            v
                    +---------------+
                    |  PostgreSQL   |
                    |   order_db    |
                    +---------------+
```

Kafka topics:

```text
order-saga
order-compensation
```

## 16. Implemented Components

```text
KafkaConfig.java

OrderSagaEvent.java
OrderSagaProducer.java
OrderSagaConsumer.java

OrderCompensationEvent.java
OrderCompensationProducer.java
OrderCompensationConsumer.java

OrderItem.java
OrderItemRepository.java
```

## 17. Testing Results

| Test | Result |
|---|---|
| Local transaction | PASSED |
| Rollback demonstration | PASSED |
| Compensation flow | PASSED |
| Saga success | PASSED |
| Saga failure | PASSED |
| Kafka producer/consumer communication | PASSED |
| PostgreSQL status update | PASSED |

Saga success:

```text
PENDING → COMPLETED
```

Saga failure:

```text
PENDING
   ↓
SAGA_FAILED
   ↓
Compensation
   ↓
CANCELLED
```

## 18. Key Findings

- Local transactions are suitable when operations belong to the same transaction boundary.
- Distributed transactions become more complex when operations span independently managed services or databases.
- 2PC provides coordinated transaction processing but introduces additional coordination and operational complexity.
- Event-driven microservices can use eventual consistency to coordinate state changes across service boundaries.
- Compensation provides a business-level recovery mechanism when a distributed workflow cannot be rolled back as a single database transaction.
- Saga decomposes a distributed workflow into local transactions and uses events and compensating actions to handle failures.

## 19. Deliverables

| Deliverable | Status |
|---|---|
| Local transaction implementation | Completed |
| Rollback demonstration | Completed |
| ACID documentation | Completed |
| Distributed transaction analysis | Completed |
| Two-Phase Commit study | Completed |
| Eventual consistency study | Completed |
| Compensation implementation | Completed |
| Saga implementation | Completed |
| Failure scenarios | Completed |
| Transaction architecture document | Completed |
| Kafka-based Saga testing | Completed |
| Compensation testing | Completed |

## 20. Conclusion

The Order Service was used to study and demonstrate transaction management in a microservices environment.

The implementation covers local database transactions using Spring `@Transactional`, Kafka-based event communication, Saga processing and compensating actions.

The tested Saga success scenario resulted in:

```text
PENDING → COMPLETED
```

The tested Saga failure scenario resulted in:

```text
PENDING
   ↓
SAGA_FAILED
   ↓
Compensation
   ↓
CANCELLED
```

The implementation demonstrates how local transactions, event-driven communication, Saga processing and compensation can be combined to manage distributed business workflows.
