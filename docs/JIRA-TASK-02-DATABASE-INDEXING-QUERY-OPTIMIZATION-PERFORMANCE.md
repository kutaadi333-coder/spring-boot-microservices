# JIRA TASK 02 — Database Indexing, Query Optimization & Performance

**Date:** 29-Sep-2026  
**Effort:** 8 Hours  
**Priority:** High

## Objective
Identify database query-performance bottlenecks in the Order database, add appropriate indexes, inspect PostgreSQL execution plans, and document optimization techniques.

## 1. Sample Dataset
The `orders` table initially contained 7 orders. A further 10,000 test orders were generated, resulting in **10,007 total orders**.

The generated data was distributed across multiple `user_id` values so user-specific performance tests were meaningful.

## 2. Tested Fields
The practical tests focused on:
- `user_id`
- `status`
- `created_at`
- composite `(user_id, created_at)`

## 3. user_id — Before Index
```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM orders
WHERE user_id = 50;
```

Observed plan:
```text
Seq Scan on orders
Filter: (user_id = 50)
Rows Removed by Filter: 9907
```

## 4. user_id Index
Created:
```sql
CREATE INDEX idx_orders_user_id
ON orders(user_id);
```

Running the same query afterward produced:
```text
Bitmap Heap Scan on orders
Bitmap Index Scan on idx_orders_user_id
Index Cond: (user_id = 50)
```

This demonstrates index-assisted access instead of a full sequential scan.

## 5. Composite Index — Before
Query:
```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM orders
WHERE user_id = 50
  AND created_at >= CURRENT_TIMESTAMP - INTERVAL '1 day';
```

Before the composite index, PostgreSQL used the `user_id` index and then filtered on `created_at`:
```text
Bitmap Index Scan on idx_orders_user_id
Filter: created_at >= ...
Rows Removed by Filter: 86
```

## 6. Composite Index — After
Created:
```sql
CREATE INDEX idx_orders_user_id_created_at
ON orders(user_id, created_at);
```

The same query then used:
```text
Bitmap Index Scan on idx_orders_user_id_created_at
Index Cond:
((user_id = 50)
 AND (created_at >= CURRENT_TIMESTAMP - INTERVAL '1 day'))
```

This demonstrates a composite index being used for both filtering conditions.

## 7. Avoiding SELECT *
Optimized query:
```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, user_id, amount, status, created_at
FROM orders
WHERE user_id = 50
  AND created_at >= CURRENT_TIMESTAMP - INTERVAL '1 day';
```

Observed plan width changed from approximately `59` to `38`, while the composite index remained in use. Single-run timings can vary with cache state.

## 8. Pagination
Query:
```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, user_id, amount, status, created_at
FROM orders
WHERE user_id = 50
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;
```

Observed:
```text
Index Scan Backward using idx_orders_user_id_created_at
Limit ... rows=20
```

This demonstrates database-level pagination using the composite index.

## 9. status — Before Index
Query:
```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, user_id, amount, status, created_at
FROM orders
WHERE status = 'PENDING';
```

Observed:
```text
Seq Scan on orders
Rows Removed by Filter: 6668
Execution Time: 5.165 ms
```

The query returned 3,339 `PENDING` orders.

## 10. status Index — After
Created:
```sql
CREATE INDEX idx_orders_status
ON orders(status);
```

The same query then used:
```text
Bitmap Heap Scan on orders
Bitmap Index Scan on idx_orders_status
Index Cond: status = 'PENDING'
```

The query again returned 3,339 rows. The observed heap-scan endpoint was approximately 1.245 ms. Exact elapsed time can vary with caching.

## 11. Final Index Definitions
Verified with `pg_indexes`:

```text
orders_pkey
idx_orders_user_id
idx_orders_user_id_created_at
idx_orders_status
```

Verification query:
```sql
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'orders'
ORDER BY indexname;
```

## 12. Performance Evidence Summary
| Experiment | Before | After |
|---|---|---|
| `user_id = 50` | Sequential Scan | Bitmap Index Scan using `idx_orders_user_id` |
| `user_id + created_at` | Single-column index + date filter | Composite index `idx_orders_user_id_created_at` |
| `status = 'PENDING'` | Sequential Scan, 5.165 ms observed | Bitmap Index Scan using `idx_orders_status`, ~1.245 ms heap-scan endpoint observed |
| Column selection | `SELECT *`, width ≈ 59 | Required columns, width ≈ 38 |
| Pagination | Not applied | Backward composite-index scan + `LIMIT 20` |

## 13. Query Optimization Concepts Demonstrated
- Avoid unnecessary `SELECT *`
- Pagination with `LIMIT/OFFSET`
- Database-side filtering with `WHERE`
- Query-plan analysis with `EXPLAIN (ANALYZE, BUFFERS)`
- Single-column indexes
- Composite indexes
- Verification of final index definitions

## 14. Completion Status
| Requirement | Status |
|---|---|
| Understand indexes | Completed |
| Create 10,000+ sample orders | Completed — 10,007 total |
| Identify frequently queried fields | Completed |
| Before-index query plan | Completed |
| Add indexes | Completed |
| After-index query plan | Completed |
| Composite index study | Completed |
| Avoid unnecessary `SELECT *` | Completed |
| Pagination example | Completed |
| Database execution plans | Completed |
| Performance comparison evidence | Completed |
| Index definitions verified | Completed |
| Performance report | Completed |

## Final Result
JIRA TASK 02 was completed by generating 10,007 orders, measuring query plans before and after indexes, implementing single-column and composite indexes, demonstrating selective-column queries and pagination, and documenting the actual PostgreSQL execution-plan evidence.
