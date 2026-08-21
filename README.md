# Secure-REST-API
Secure REST API simulating a banking authentication and transaction system, built with Spring Boot to demonstrate industry-standard security practices in Java.

# Secure Vault

Banking REST API focused on security — authentication, transactions,
and auditing — built with Spring Boot to demonstrate industry best
practices in Java.

## Stack

- Java 21 · Spring Boot 3.3
- Spring Data JPA (H2 dev / PostgreSQL prod-ready)
- Spring Security + JWT
- JUnit 5 + Mockito

## Architecture

`controller` → `service` → `repository (JPA)` → `model (Entity)`  
Cross-cutting security in `security/` (JWT filters, Spring Security config).

## Implemented Security

- [ ] Password hashing (PasswordEncoder / BCrypt)
- [ ] Stateless authentication with JWT
- [ ] SQL Injection prevention (parameterized JPA)
- [ ] Lockout on failed login attempts
- [ ] Structured auditing (Logback)
- [ ] Centralized exception handling (@ControllerAdvice)

## How to Run
```bash
mvn spring-boot:run
```

## Tests
```bash
mvn test
```
