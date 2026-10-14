# JIRA Task 02 - API Gateway Security & Request Policies

## Objective
Implement and validate API Gateway request routing, authentication,
Redis-based rate limiting, request logging, and error handling.

## Implementation
- Configured Spring Cloud Gateway Server WebMVC routes to downstream services.
- Configured the imperative HTTP client to use the JDK implementation.
- Configured HTTP connection and response timeouts.
- Added Gateway HTTP Basic authentication for protected routes.
- Kept designated health, information, product-public, and authentication
  endpoints accessible without Gateway Basic authentication.
- Implemented Redis-based rate limiting using StringRedisTemplate.
- Configured a maximum of 100 requests per client in a 60-second window.
- Returns HTTP 429 Too Many Requests when the limit is exceeded.
- Returns HTTP 503 if Redis rate-limit operations fail.
- Added correlation ID and Gateway request/response logging.
- Added exception logging for downstream request failures.

## Validation Results

| Test | Expected result | Observed result |
|---|---|---|
| Gateway startup | Application starts | Passed |
| User API through Gateway | HTTP 200 | Passed |
| 10 repeated Gateway requests | HTTP 200 | Passed |
| 10 direct User Service requests | HTTP 200 | Passed |
| Request without Gateway credentials | HTTP 401 | Passed |
| Request with valid Basic Auth credentials | HTTP 200 | Passed |
| Redis rate-limit counter and window | Counter increments; 60-second window | Passed |
| Requests 1-100 with one client ID | HTTP 200 | Passed |
| Request 101 with the same client ID | HTTP 429 | Passed |
| Public Product endpoint | HTTP 200 | Previously verified |

## Rate-Limiting Policy
The Gateway uses the X-User-ID header as the rate-limit client identifier.
If that header is absent, it falls back to the remote IP address.

Redis maintains a per-client counter with a 60-second expiry. Requests above
the configured limit receive HTTP 429 and a Retry-After: 60 header.

## Security Notes
- Gateway HTTP Basic authentication was tested successfully.
- The current Gateway authentication implementation uses an in-memory
  development user. It is not JWT validation.
- Production deployment requires secure credential management and HTTPS.
- Client-supplied X-User-ID values should not be trusted as authenticated
  identities without validation.
- Gateway authentication and authorization should be reviewed against the
  final role-based access requirements.

## Remaining Considerations
- Automated test-suite execution has not been confirmed.
- Gateway authorization by business role has not been fully verified.
- Public endpoint access should be reviewed against the final security policy.

## Conclusion
The repeated-request timeout was addressed by changing the Gateway's
imperative HTTP client configuration to use the JDK implementation.
Repeated-request validation succeeded. Redis rate limiting was verified
with HTTP 429 on request 101 after requests 1-100 returned HTTP 200.
Gateway Basic authentication was also tested successfully.
