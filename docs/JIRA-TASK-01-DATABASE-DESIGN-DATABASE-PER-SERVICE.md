# JIRA TASK 01 — Database Design & Database-per-Service Architecture

**Date:** 28-Sep-2026  
**Effort:** 8 Hours  
**Priority:** Critical

## 1. Objective

Design and document independently owned databases for the microservices currently used in the project.

The implementation was verified against the running PostgreSQL databases and the existing service entities.

## 2. Database-per-Service Architecture

```text
User Service
    |
    v
 user_db
    |
    +-- user_details

Order Service
    |
    v
 order_db
    |
    +-- orders
    |
    +-- order_items

Payment
    |
    v
 payment_db
    |
    +-- payments
```

The Payment database was created for the database-design requirement. A separate Payment Service is not currently implemented in the project.

## 3. User Database

### Database
```text
user_db
```

### Table
```text
user_details
```

### Verified columns
```text
id
name
email
phone
password
status
created_at
updated_at
```

The existing `UserDetails` entity maps to `user_details`. The `email` field is unique, and `status`, `createdAt`, and `updatedAt` are initialized by the entity lifecycle methods.

### Ownership
```text
User Service
     |
     v
user_db
     |
     v
user_details
```

## 4. Order Database

### Database
```text
order_db
```

### Tables
```text
orders
order_items
```

### `orders` — verified columns
```text
id
user_id
product_name
quantity
amount
status
created_at
```

### `order_items` — verified columns
```text
id
order_id
product_name
quantity
amount
```

### Schema
```text
orders
+----------------+
| id             | PK
| user_id        |
| product_name   |
| quantity       |
| amount         |
| status         |
| created_at     |
+----------------+

order_items
+----------------+
| id             | PK
| order_id       |
| product_name   |
| quantity       |
| amount         |
+----------------+
```

The `OrderItem` entity was added to create the required `order_items` table in the Order database.

### Ownership
```text
Order Service
     |
     v
order_db
   /        v        v
orders  order_items
```

## 5. Payment Database

### Database
```text
payment_db
```

### Table
```text
payments
```

### Verified columns
```text
id
order_id
amount
payment_status
transaction_reference
created_at
```

### Schema
```text
payments
+-----------------------+
| id                    | PK
| order_id              |
| amount                |
| payment_status        |
| transaction_reference |
| created_at            |
+-----------------------+
```

The `payments` table was created directly in PostgreSQL because a standalone Payment Service is not currently present in the project.

### Ownership
```text
Payment domain
      |
      v
payment_db
      |
      v
payments
```

## 6. Data Ownership

```text
User Service   -> user_db / user_details
Order Service  -> order_db / orders / order_items
Payment domain -> payment_db / payments
```

Each service/domain owns its own tables.

## 7. Service Communication Instead of Direct Database Access

Order Service should obtain user information through the User Service API rather than querying `user_db` directly.

```text
Order Service
      |
      | REST / User API
      v
User Service
      |
      v
user_db
      |
      v
user_details
```

The current Order entity stores `user_id` as an identifier and does not contain a JPA relationship to the User database.

## 8. Database Separation

```text
+------------------+      +---------+
|   User Service   | ---> | user_db |
+------------------+      +---------+

+------------------+      +----------+
|  Order Service   | ---> | order_db |
+------------------+      +----------+

+------------------+      +------------+
|  Payment Domain  | ---> | payment_db |
+------------------+      +------------+
```

No shared table was created between these databases.

## 9. ER-style Diagrams

### User DB
```text
+----------------------------+
|        user_details        |
+----------------------------+
| id           PK            |
| name                       |
| email        UNIQUE        |
| phone                      |
| password                   |
| status                     |
| created_at                 |
| updated_at                 |
+----------------------------+
```

### Order DB
```text
+----------------------------+       +----------------------------+
|           orders           |       |        order_items         |
+----------------------------+       +----------------------------+
| id           PK            |       | id           PK            |
| user_id                    |       | order_id                   |
| product_name               |       | product_name               |
| quantity                   |       | quantity                   |
| amount                     |       | amount                     |
| status                     |       | amount                     |
| created_at                 |       +----------------------------+
+----------------------------+
```

`order_id` in `order_items` is the order identifier used by the application schema. No cross-database foreign key was created.

### Payment DB
```text
+----------------------------+
|          payments          |
+----------------------------+
| id                  PK     |
| order_id                   |
| amount                     |
| payment_status             |
| transaction_reference      |
| created_at                 |
+----------------------------+
```

## 10. Verification Evidence

The schemas were verified in pgAdmin against PostgreSQL 16.

```text
order_db
  -> public
     -> orders
     -> order_items

user_db
  -> public
     -> user_details

payment_db
  -> public
     -> payments
```

After creating `OrderItem`, Order Service was restarted successfully and Hibernate created the `order_items` table. Order Service also started on port `8082` and registered with Eureka.

## 11. Payment SQL Schema

The following SQL was used to create the Payment database table:

```sql
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount NUMERIC(38,2) NOT NULL,
    payment_status VARCHAR(50) NOT NULL,
    transaction_reference VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## 12. Completion Status

| Requirement | Status |
|---|---|
| Database design | Completed |
| Database-per-service structure | Completed |
| User database schema | Completed |
| Order database schema | Completed |
| `order_items` table | Completed |
| Payment database | Completed |
| `payments` table | Completed |
| Data ownership definition | Completed |
| Direct User DB dependency avoided in Order entity | Completed |
| Database verification in pgAdmin | Completed |
| Architecture document | Completed |
| ER-style diagrams | Completed |
| SQL schema evidence | Completed |

## 13. Notes

The task names the User table conceptually as `users`; the existing working User Service uses `user_details`. The existing working table was retained rather than renaming a table used by authentication.

The task includes a Payment database as part of the database architecture. The current project does not yet contain a standalone Payment Service, so the Payment database/schema was prepared directly in PostgreSQL. The Payment Service implementation belongs to the later Saga/distributed workflow task.

## 14. Task 01 Summary

```text
User Service
      |
      v
    user_db
      |
      v
 user_details


Order Service
      |
      v
   order_db
    /       v      v
orders  order_items


Payment Domain
      |
      v
  payment_db
      |
      v
  payments
```

This completes the database design and database-per-service architecture portion of JIRA Task 01 based on the current project implementation and verified PostgreSQL schemas.
