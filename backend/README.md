# Dagantara — Spring Boot Foundation

Java 21, Spring Boot 3.5.16, Maven, PostgreSQL. This is a reusable technical foundation; no business-domain endpoints or schemas are introduced.

## Scope

Included: OpenAPI/Swagger, success response contract and paged metadata, ProblemDetail exception handling with stable error codes, Jakarta request validation, PostgreSQL/JPA, dev/staging/prod profiles, structured request ID in logs, Spring Security, JWT access tokens and a temporary technical bootstrap login, CORS, pageable/sort allowlist helper, timestamp auditing, Actuator health probes, /api/v1 routing convention, mapper interface, UUID base entity, @Transactional infrastructure, and a multi-stage non-root Dockerfile.

Excluded by decision: database migration tooling, test harness, Redis/cache, rate limiting, file storage, background jobs, multi-tenant context and RBAC. Existing domain specification and agent skill documents remain unchanged and apply to later feature work.

## Development

1. Install JDK 21, Maven 3.6.3+, and PostgreSQL.
2. Create an empty PostgreSQL database named dagantara and grant your local database user access.
3. Copy `.env.example` to `.env`. Set `DB_*`, `APP_JWT_SECRET_BASE64`, and optionally the bootstrap username and BCrypt hash. Spring does **not** load `.env` automatically.
4. Generate a signing secret: `openssl rand -base64 48`. Do not reuse one secret across environments.
5. To generate a BCrypt hash on a machine with Apache htpasswd installed: `htpasswd -nBC 12 bootstrap`. Enter the password interactively, and copy only the hash portion following `bootstrap:`. Put the hash in single quotes in the shell/.env so dollar signs remain literal.
6. From `backend/`, export environment values and run:

```bash
set -a
. ./.env
set +a
mvn spring-boot:run
```

The application **requires a valid JWT signing secret on startup**, even when only checking health; an absent/weak secret fails closed. The username/password hash may be blank, in which case all login attempts return 401.

This project intentionally has **no migration tool or automatic schema generation** (`ddl-auto=none`). An empty database works for the current skeleton. When real entities are added, prepare their tables deliberately through the separately chosen database administration process; never depend on automatic production `update`.

## Endpoints

| Method | Path | Authentication | Notes |
| --- | --- | --- | --- |
| GET | /actuator/health | Public | Overall health (DB connectivity affects status) |
| GET | /actuator/health/liveness | Public | Liveness probe |
| GET | /actuator/health/readiness | Public | Readiness probe |
| GET | /swagger-ui/index.html | Public in dev only | Disabled in common/staging/prod profiles |
| GET | /v3/api-docs | Public in dev only | Disabled in common/staging/prod profiles |
| POST | /api/v1/auth/login | Public | Validates bootstrap credentials; returns JWT |
| All other /api/v1/** | Bearer JWT | Protected | No domain endpoints yet |

Example login:

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"bootstrap","password":"<your-password>"}'
```

Sample success:

```json
{"success":true,"message":"Login successful","data":{"accessToken":"<jwt>","tokenType":"Bearer","expiresAt":"2026-11-19T12:30:00Z"},"meta":null}
```

Standard errors use `application/problem+json`, with `type`, `title`, `status`, `detail`, `instance`, `errorCode` and `requestId`. Validation errors also include `violations`. Do not log credentials or tokens. Each request receives an `X-Request-ID` header, and its value appears in application logs.

## JSON and data conventions

- Use `ApiResponse.success(message, dto)` for single-resource and command success, or `ApiResponse.paged(message, page)` for Page responses.
- Use request/response DTOs and the `DtoMapper<S,T>` contract. Never serialize JPA entities directly.
- Use `PageableRules.enforce(pageable, Set.of("name", "createdAt"))` in list services to enforce the sort-field allowlist; pageable caps at 100.
- Extend `AuditableEntity` where UUID identifiers plus `createdAt`/`updatedAt` fit the domain. Timestamps are stored as Instant/UTC. This mapped superclass does not create any table itself.
- Mark transactional business service methods with `@Transactional`; repository/configuration wiring is provided by Spring Data JPA.
- Use `ApiRoutes.V1` for new versioned controllers and explicit `ApiException` codes for domain errors.

## Authentication limitations

Bootstrap authentication is intentionally **single-credential and infrastructure-only**. There is no self-registration, user-table provisioning, refresh token, token revocation, roles or permissions. The bootstrap username and password hash are environment configuration; the JWT is signed with HS256, validates signature, expiry, issuer and audience, and has configurable short lifetime. Replace `BootstrapCredentials` with a backed identity service once the domain User Management specification is implemented, without exposing an open registration endpoint.

This foundation has no application-level login rate limiting by explicit scope decision. If exposing it to an untrusted network, provide login abuse protection upstream before production release. Configure `APP_CORS_ALLOWED_ORIGINS` explicitly for every allowed frontend origin; an empty list allows no cross-origin browser requests.

## Docker

```bash
docker build -t dagantara-backend .
docker run --rm --env-file .env -p 8080:8080 dagantara-backend
```

For Docker, `DB_URL` must point to a PostgreSQL host reachable from the container (not the container's own localhost). Secrets should be injected through a secrets manager or environment configuration, not baked into the image.

## Verification

`mvn -DskipTests package` compiles and packages locally, and `curl -f http://localhost:8080/actuator/health` checks runtime health. No automated test suite or CI is added by design. A successful build or runtime test must not be assumed until actually executed.
