# Secure Vault

Secure banking-style REST API built with Spring Boot to demonstrate
industry-standard security practices in Java — authentication,
account management, and transactions, developed as a portfolio
project for a junior Java developer interview.

## Stack

- Java 21 · Spring Boot 3.x
- Spring Data JPA (H2 in-memory for dev)
- Spring Security + JWT (stateless authentication)
- BCrypt password hashing
- JUnit 5 + Mockito (unit testing)
- Maven

## Architecture
`controller` → `service` → `repository (JPA)` → `model (Entity)`  

Security is handled transversally in `security/` (JWT filter, provider
config, custom UserDetailsService). Cross-cutting error handling lives
in `exception/` via `@RestControllerAdvice`.

## Security implemented

- [x] Password hashing with BCrypt (`PasswordEncoder`) — never stored in plain text
- [x] Stateless authentication with JWT (`SessionCreationPolicy.STATELESS`)
- [x] SQL Injection prevention via Spring Data JPA (parameterized queries)
- [x] Brute-force protection — account lockout after 5 failed login attempts
- [x] Same error message for "user not found" and "wrong password" (prevents user enumeration)
- [x] Role assigned server-side only (`CLIENT` by default — no client-controlled role escalation on registration)
- [x] Structured audit logging (SLF4J/Logback) — no `printStackTrace`, no sensitive data logged
- [x] Centralized exception handling (`@RestControllerAdvice`) — no internal error details leaked to clients
- [x] `BigDecimal` for all monetary amounts (never `double`/`float`)
- [ ] Authorization check: verify account ownership before deposit/withdraw/transfer *(known gap — see below)*
- [ ] Migrate `ddl-auto: update` to Flyway/Liquibase for production-safe schema migrations

## Known limitations / next steps

- **Authorization gap**: any authenticated user can currently operate on any
  account number if guessed, since account ownership isn't yet verified
  against the authenticated user. Identified deliberately as a discussion
  point — next step is either a manual ownership check in `AccountController`
  or `@PreAuthorize` with a custom permission evaluator.
- No `GET /api/accounts/{accountNumber}` endpoint yet to check balance
  without performing an operation.

## How to run

```bash
mvn spring-boot:run
```

API available at `http://localhost:8080`.

## Testing

```bash
mvn test
```

17 unit tests (JUnit 5 + Mockito), covering:
- `AuthServiceTest` (7 tests): registration, login, brute-force lockout, boundary case at the 5th failed attempt
- `AccountServiceTest` (9 tests): deposit, withdrawal, transfer, insufficient funds, exact-balance boundary case

All service-layer tests are pure unit tests — repositories are mocked,
no database is touched.

### Manual API testing

See [`secure-vault.postman_collection.json`](./secure-vault.postman_collection.json)
for a ready-to-import Postman collection covering the full flow
(register → login → create account → deposit/withdraw/transfer).

## Project status

🚧 Core functionality complete and tested. Actively closing the
authorization gap above as the next milestone.
