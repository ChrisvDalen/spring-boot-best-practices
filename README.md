# Spring Boot Best Practices

A runnable reference project demonstrating Spring Boot best practices, one subject per package. Every file contains inline comments explaining **why** each decision was made, not just **what** the code does.

## Quick Start

```bash
git clone https://github.com/ChrisvDalen/spring-boot-best-practices.git
cd spring-boot-best-practices
./mvnw spring-boot:run
```

- API: `http://localhost:8080/api/v1/users`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:bestpractices`)
- Actuator: `http://localhost:8080/actuator/health`

## Best-Practice Index

| # | Subject | Key files |
|---|---------|-----------|
| 1 | [Project structure](#1-project-structure) | `user/` package |
| 2 | [REST API design](#2-rest-api-design) | `UserController.java` |
| 3 | [Data access / JPA](#3-data-access--jpa) | `User.java`, `UserRepository.java` |
| 4 | [Service layer](#4-service-layer) | `UserService.java` |
| 5 | [Validation](#5-validation) | `ValidEmail.java`, `EmailValidator.java`, DTOs |
| 6 | [Error handling](#6-error-handling) | `GlobalExceptionHandler.java`, `ErrorResponse.java` |
| 7 | [Caching](#7-caching) | `CacheConfig.java`, `@Cacheable` in `UserService` |
| 8 | [Async processing](#8-async-processing) | `AsyncConfig.java`, `NotificationService.java` |
| 9 | [Security](#9-security) | `SecurityConfig.java` |
| 10 | [Logging & tracing](#10-logging--tracing) | `MdcLoggingFilter.java`, `application.yml` |
| 11 | [Actuator & observability](#11-actuator--observability) | `AppHealthIndicator.java` |
| 12 | [Configuration & profiles](#12-configuration--profiles) | `application*.yml` |
| 13 | [Testing](#13-testing) | `UserServiceTest`, `UserControllerTest`, `UserRepositoryTest` |
| 14 | [Docker & containerisation](#14-docker--containerisation) | `Dockerfile`, `docker-compose.yml` |

---

### 1. Project structure

Packages are organised **by feature** (`user/`), not by layer (`controller/`, `service/`, …`). Feature packaging keeps related code together, makes it easy to delete a feature without hunting across the codebase, and scales better as the project grows.

```
src/main/java/com/example/bestpractices/
├── user/                   ← full vertical slice (entity → controller)
│   └── dto/                ← request/response DTOs live next to their feature
├── config/                 ← cross-cutting Spring configuration
├── security/
├── exception/
├── logging/
├── actuator/
├── validation/
└── async/
```

---

### 2. REST API design

**File:** [`UserController.java`](src/main/java/com/example/bestpractices/user/UserController.java)

- Version the path from day one: `/api/v1/...`
- Use the correct HTTP verb and status code (POST → 201, DELETE → 204)
- Return a `Location` header on 201 pointing to the new resource
- Accept `Pageable` on list endpoints; annotate with `@PageableDefault` for safe defaults
- Controllers are thin — no business logic, only HTTP ↔ service translation

---

### 3. Data access / JPA

**Files:** [`User.java`](src/main/java/com/example/bestpractices/user/User.java), [`UserRepository.java`](src/main/java/com/example/bestpractices/user/UserRepository.java), [`JpaConfig.java`](src/main/java/com/example/bestpractices/config/JpaConfig.java)

- Table name `users` — avoids the SQL reserved word `user`
- Use `Instant` for timestamps to avoid timezone ambiguity
- Enable JPA Auditing (`@EnableJpaAuditing`) for automatic `createdAt` / `updatedAt`
- Return `Optional<T>` from single-result finders; callers must handle absence explicitly
- Accept `Pageable` to prevent unbounded result sets

---

### 4. Service layer

**File:** [`UserService.java`](src/main/java/com/example/bestpractices/user/UserService.java)

- Use `@Transactional(readOnly = true)` as the class default; override on write methods
- Constructor injection via `@RequiredArgsConstructor` — avoids field injection issues with testing and immutability
- The service layer owns entity → DTO mapping; entities never leave the service
- Validate uniqueness before calling `save()` to surface clear error messages

---

### 5. Validation

**Files:** [`ValidEmail.java`](src/main/java/com/example/bestpractices/validation/ValidEmail.java), [`EmailValidator.java`](src/main/java/com/example/bestpractices/validation/EmailValidator.java), request DTOs

- Declare constraints on DTOs, not on JPA entities
- `@NotBlank` (not `@NotNull`) rejects blank whitespace strings
- Custom `@ValidEmail` shows how to build reusable, composable constraint annotations
- Regex compiled once as `static final` — not on every validation call
- Null-safe validator: pair `@ValidEmail` with `@NotBlank` rather than coupling both into one annotation

---

### 6. Error handling

**Files:** [`GlobalExceptionHandler.java`](src/main/java/com/example/bestpractices/exception/GlobalExceptionHandler.java), [`ErrorResponse.java`](src/main/java/com/example/bestpractices/exception/ErrorResponse.java)

- `@RestControllerAdvice` centralises all HTTP error mapping
- Consistent `ErrorResponse` envelope across all error types
- Validation errors return per-field details (`errors[]` array)
- Correlation ID from MDC is included in every error response for log tracing
- Generic `Exception` catch-all prevents stack traces leaking to the client

---

### 7. Caching

**Files:** [`CacheConfig.java`](src/main/java/com/example/bestpractices/config/CacheConfig.java), [`UserService.java`](src/main/java/com/example/bestpractices/user/UserService.java)

- Use Caffeine for single-instance apps; swap for Redis when scaling horizontally
- Always set a TTL — an unbounded cache is a memory leak
- Set `maximumSize` to cap memory under load
- `@Cacheable` on reads, `@CachePut` on updates, `@CacheEvict` on deletes
- Cache names as constants prevent silent cache misses from typos

---

### 8. Async processing

**Files:** [`AsyncConfig.java`](src/main/java/com/example/bestpractices/config/AsyncConfig.java), [`NotificationService.java`](src/main/java/com/example/bestpractices/async/NotificationService.java)

- Use a named `ThreadPoolTaskExecutor` — never the default `SimpleAsyncTaskExecutor`
- Size the pool for the workload: IO-bound tasks can use more threads than CPU-bound
- Set `queueCapacity` to apply back-pressure
- Return `CompletableFuture<T>` when callers need the result; `void` for fire-and-forget
- `@Async` only works when called from outside the bean (proxied method call)

---

### 9. Security

**File:** [`SecurityConfig.java`](src/main/java/com/example/bestpractices/security/SecurityConfig.java)

- Use `SecurityFilterChain` @Bean — `WebSecurityConfigurerAdapter` was removed in Spring Boot 3
- Disable CSRF for stateless REST APIs; use STATELESS session policy
- Allow public access to actuator health, OpenAPI docs explicitly; lock down everything else
- Separate read (GET) and write permissions for fine-grained RBAC

---

### 10. Logging & tracing

**Files:** [`MdcLoggingFilter.java`](src/main/java/com/example/bestpractices/logging/MdcLoggingFilter.java), [`application.yml`](src/main/resources/application.yml)

- Servlet filter runs before Spring MVC — every request gets a correlation ID
- Honour upstream `X-Correlation-ID` to preserve distributed traces end-to-end
- Store the ID in MDC so every log line in the thread includes it automatically
- Echo the ID in the response header so clients can include it in bug reports
- Always `MDC.remove()` in `finally` to prevent ID leaking to the next request on a pooled thread

---

### 11. Actuator & observability

**File:** [`AppHealthIndicator.java`](src/main/java/com/example/bestpractices/actuator/AppHealthIndicator.java)

- Implement `HealthIndicator` for domain-level health checks beyond simple UP/DOWN
- Keep health checks cheap — no heavy queries or slow external calls
- Expose `health`, `info`, `metrics`, and `caches` endpoints; restrict in production
- Use `management.info.env.enabled=true` with `info.*` properties for `/actuator/info`

---

### 12. Configuration & profiles

**Files:** [`application.yml`](src/main/resources/application.yml), [`application-dev.yml`](src/main/resources/application-dev.yml), [`application-prod.yml`](src/main/resources/application-prod.yml)

- Use YAML over `.properties` for hierarchical readability
- Base config holds safe local defaults; profiles add or override for each environment
- Production secrets are environment variables, never hardcoded
- Use `ddl-auto: validate` in production; run Flyway or Liquibase for migrations
- Log pattern includes `%X{correlationId}` from MDC

---

### 13. Testing

**Files:** [`UserServiceTest`](src/test/java/com/example/bestpractices/user/UserServiceTest.java), [`UserControllerTest`](src/test/java/com/example/bestpractices/user/UserControllerTest.java), [`UserRepositoryTest`](src/test/java/com/example/bestpractices/user/UserRepositoryTest.java)

| Test class | Annotation | What loads |
|---|---|---|
| `UserServiceTest` | `@ExtendWith(MockitoExtension.class)` | No Spring context — fastest |
| `UserControllerTest` | `@WebMvcTest` | Web layer only (MockMvc) |
| `UserRepositoryTest` | `@DataJpaTest` | JPA + embedded H2 only |

- Test behaviour, not implementation
- Use AssertJ for readable assertions
- `@WebMvcTest` tests the real security rules and HTTP contract (status codes, headers, body shape)
- `@DataJpaTest` tests custom `@Query` methods and JPA auditing

---

### 14. Docker & containerisation

**Files:** [`Dockerfile`](Dockerfile), [`docker-compose.yml`](docker-compose.yml)

- Multi-stage build: JDK compiles, JRE runs — smaller final image
- Layered JAR extraction: infrequently-changing dependency layers are Docker-cached separately
- Non-root user in the runtime image reduces blast radius
- `docker-compose.yml` uses a Postgres health check so the app waits for the DB to be ready
- Secrets injected via environment variables with safe local defaults

---

## Running Tests

```bash
./mvnw test
```

## Running with Docker

```bash
docker compose up --build
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Security

See [SECURITY.md](SECURITY.md).

## License

MIT — see [LICENSE](LICENSE).
