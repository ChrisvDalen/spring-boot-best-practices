# Changelog

All notable changes to this project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versioning follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial project scaffold with Spring Boot 3.2 and Java 21
- User CRUD REST API (`/api/v1/users`) with pagination
- Bean Validation with custom `@ValidEmail` annotation
- Global exception handler returning consistent `ErrorResponse` envelope
- Spring Cache (Caffeine) on `UserService`
- Async `NotificationService` with dedicated thread pool
- MDC logging filter for per-request correlation IDs
- Custom Actuator `HealthIndicator`
- Spring Security config (stateless, JWT-ready)
- OpenAPI / Swagger UI via springdoc
- Multi-stage layered Dockerfile
- `docker-compose.yml` with Postgres and health checks
- Dev / prod Spring profiles
- Unit, controller-slice, and repository-slice tests
