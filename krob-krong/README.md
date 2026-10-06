# Krob Krong

Backend for a multi-tenant school platform: identity and access management (IAM), schools and their branches,
workspaces, and media. Each school is a tenant; people get roles in a school (optionally limited to one branch),
and every API decision is made by a role-based access control (RBAC) policy evaluated in the application.

| | |
|---|---|
| Runtime | Java 25, Spring Boot 4.1, Spring Security 7, Hibernate 7 |
| Database | PostgreSQL 17 with TimescaleDB, migrated by Flyway |
| Object storage | Any S3-compatible store; RustFS locally |
| Cache | Valkey (available locally, not used by the app yet) |
| Build | Gradle, Spotless (palantir-java-format), Lombok, MapStruct |

## Contents

- [Quick start](#quick-start)
- [Project layout](#project-layout)
- [Conventions](#conventions)
- [Security and authorization](#security-and-authorization)
  - [`@RequirePermission`](#requirepermission)
  - [The current user](#the-current-user)
  - [Checks that need request data](#checks-that-need-request-data)
  - [Adding a permission](#adding-a-permission)
  - [Tokens](#tokens)
  - [Normal-user authentication](#normal-user-authentication)
- [Common utilities](#common-utilities)
  - [API responses and paging](#api-responses-and-paging)
  - [Errors and exceptions](#errors-and-exceptions)
  - [S3 storage (`S3Util`)](#s3-storage-s3util)
  - [Logging and masking](#logging-and-masking)
  - [Request tracing (`@RequestId`, `@TraceId`)](#request-tracing-requestid-traceid)
- [Database](#database)
- [Configuration](#configuration)
- [API overview](#api-overview)
- [School onboarding and SMTP setup](docs/onboarding.md)
- [Testing](#testing)
- [Recipe: a new protected endpoint](#recipe-a-new-protected-endpoint)
- [Known gaps](#known-gaps)

## Quick start

Prerequisites: JDK 25 and Docker.

```bash
docker compose up -d --wait   # PostgreSQL + TimescaleDB, RustFS, Valkey, Mailpit
./gradlew bootRun             # migrates the database and starts on :8080
curl localhost:8080/actuator/health
```

| Service | Address | Default credentials |
|---|---|---|
| PostgreSQL + TimescaleDB | `localhost:5432`, database `krob_krong` | `postgres` / `supersecret` |
| RustFS S3 API | `http://localhost:9000` | `krobkrong` / `krobkrong-secret` |
| RustFS console | http://localhost:9001/rustfs/console/ | same as above |
| Valkey | `localhost:6379` | password `supersecret` |
| Mailpit | SMTP `localhost:1025`, inbox http://localhost:8025 | local capture; no credentials |

Every value in [compose.yaml](docker-compose.yaml) can be overridden from a `.env` file next to it
(`POSTGRES_PASSWORD`, `RUSTFS_SECRET_KEY`, `VALKEY_PASSWORD`, `POSTGRES_PORT`, …). Data lives in named volumes;
`docker compose down -v` wipes it.

On startup the app applies migrations, validates every entity against the schema (`ddl-auto: validate`), and
creates the `krob-krong` bucket if it is missing.

## Project layout

```
src/main/java/io/sala/krob_krong
├── common/                 shared building blocks (no business rules)
│   ├── annotations/        @RequestId, @TraceId
│   ├── errors/             ErrorCode, CommonErrorCode, ApiError, ErrorCategory
│   ├── exceptions/         KrobKrongException, BusinessException, ExceptionMapper + built-in mappers,
│   │                       GlobalExceptionHandler
│   ├── filters/ trace/     request/response logging filter, MDC filter, trace argument resolver
│   ├── log/ config/        MaskedLogger, MaskingConfig
│   ├── request/ response/  PageRequest, ApiResponse, PageResponse, PageMeta
│   └── utils/              JsonMasker, XmlMasker, TraceContext, s3/ (S3Util, ETags)
├── iam/                    identity and access only
│   ├── account/            users, refresh tokens
│   ├── rbac/               roles, permissions, grants (controllers, services, specifications)
│   ├── security/           JWT, @RequirePermission, policy evaluation, error handlers, actor binding
│   ├── audit/              audit events, access log
│   ├── support/            time-boxed support sessions
│   └── error/              IAMErrorCode
├── school/                 schools and school reviews
├── branch/                 branches, workspaces, reserved slugs
└── media/                  media assets and renditions

src/main/resources
├── application.yaml
└── db/migration/           Flyway migrations, one file per responsibility
```

Each domain module follows the same internal layout:

```
<module>/
├── controller/        @RestController, thin: binds input, calls one service method
├── service/           interfaces
│   └── impl/          implementations (@Service)
├── repository/        JpaRepository + JpaSpecificationExecutor
├── specification/     static Specification factories
├── entity/            JPA entities (@FieldNameConstants)
├── dto/               request/response classes
└── mapper/            MapStruct mappers (componentModel = "spring")
```

## Conventions

**API field names use snake_case.** Jackson applies `spring.jackson.property-naming-strategy: SNAKE_CASE`
to all JSON request/response DTOs, including nested data, `request_id`, `trace_id`, and pagination metadata
(`total_elements`, `total_pages`, `has_next`). Validation field-error keys use the same naming. Multiword
query parameters are explicitly bound as `school_id` and `branch_id`; Jackson naming does not bind query
parameters. Java fields and methods retain camelCase. Map keys supplied by application code must use
snake_case when they represent API fields; protocol keys such as HTTP headers retain their defined spelling.

This replaces the previous camelCase wire contract. Clients must send `display_name`, `refresh_token`,
`user_id`, `role_id`, `branch_id`, and `valid_to` where applicable and read snake_case response fields.
Bruno environment/runtime variables use the same convention (`base_url`, `access_token`, `auth_email`, etc.).
Re-enter local secret values as `auth_password`, `access_token`, and `refresh_token` after upgrading an
existing Bruno environment. Bruno's own YAML settings and scripting methods retain their required names.

**Service interface + implementation.** Controllers and other modules depend on the interface
(`UserRoleService`), never the implementation (`UserRoleServiceImpl`).

**Queries are Specifications.** Repositories extend `JpaSpecificationExecutor`; filters are composable static
factories built on `@FieldNameConstants`, so a renamed field breaks compilation instead of a query:

```java
List<UserRoleEntity> grants = userRoleRepository.findAll(
        UserRoleSpecification.byUserId(userId)
                .and(UserRoleSpecification.bySchoolId(schoolId))
                .and(UserRoleSpecification.notRevoked())
                .and(UserRoleSpecification.fetchRole()),       // join-fetch instead of lazy loading
        Sort.by(Sort.Direction.DESC, UserRoleEntity.Fields.grantedAt));
```

**Transactions only where needed.** Put `@Transactional` on the individual method that needs atomicity — a
read-modify-write like `acceptInvitation`, or several writes that must succeed together. Never annotate a whole
class and never a controller; [TransactionBoundaryTest](src/test/java/io/sala/krob_krong/TransactionBoundaryTest.java)
fails the build if you do. A transaction holds a pool connection from method entry to commit, so:

- never call HTTP, S3, email or anything slow inside one — finish the database work, then do the I/O;
- `spring.jpa.open-in-view` is off: load what the response needs inside the query (e.g. `fetchRole()`) rather
  than relying on lazy loading after the transaction;
- a `@Transactional` method called from the same class runs without a transaction (Spring proxies are bypassed).

**Formatting.** `./gradlew spotlessApply` before committing; `spotlessCheck` runs as part of `./gradlew build`.

## Security and authorization

The API is a stateless OAuth2 resource server. Every request except `POST /api/auth/register`,
`POST /api/auth/login`, `POST /api/auth/refresh`, `/error`,
`/actuator/health` and `/actuator/info` needs `Authorization: Bearer <access token>`. Other actuator endpoints
are denied. Responses carry a strict CSP, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer` and HSTS (on
HTTPS); CORS allows only the origins in `krob-krong.security.cors.allowed-origins`.

Authentication answers *who*; tokens carry identity only. Authorization answers *what*, per request, from the
database — so a revoked grant, a suspended school or an ended support session takes effect immediately.

### `@RequirePermission`

The main tool for protecting an endpoint. Put it on a controller method:

```java
@GetMapping("/schools/{school_id}/members")
@RequirePermission(value = "member:read", school = "#schoolId")
public ApiResponse<PageResponse<MemberResponse>> listMembers(@PathVariable("school_id") UUID schoolId, PageRequest page) { … }

@GetMapping("/schools/{school_id}/members/{user_id}/roles")
@RequirePermission(value = "member:read", school = "#schoolId", target = "#userId")
public ApiResponse<List<UserRoleResponse>> listUserRoles(@PathVariable("school_id") UUID schoolId, @PathVariable("user_id") UUID userId) { … }

@PostMapping("/schools/{school_id}/review")
@RequirePermission("school:review")          // a platform permission: no school needed
public ApiResponse<…> review(…) { … }
```

| Attribute | Meaning | Default |
|---|---|---|
| `value` | Permission code, `resource:action` (e.g. `branch:edit`, `media.staff:view`) | required |
| `school` | SpEL for the school (tenant) being accessed, usually `"#schoolId"` | `"null"` |
| `branch` | SpEL for the branch being accessed | `"null"` |
| `target` | SpEL for the user the action is aimed at; lets `own`-scoped grants apply | `"null"` |

`#name` refers to a method parameter by name (the Spring Boot Gradle plugin compiles with `-parameters`); any
SpEL works, e.g. `"#request.branchId"`.

How a check is decided ([RbacAuthorizer](src/main/java/io/sala/krob_krong/iam/security/RbacAuthorizer.java) →
[AccessPolicyService](src/main/java/io/sala/krob_krong/iam/security/AccessPolicyService.java) →
[AccessContext](src/main/java/io/sala/krob_krong/iam/security/policy/AccessContext.java)): the caller is allowed if
any role they hold grants the permission in a scope that fits the request.

| Scope | Allows | Typical roles |
|---|---|---|
| `public` | Anyone, but only for approved schools and active branches, and not aimed at a specific user | `visitor` |
| `own` | Acting on yourself (`target` equals the caller) | `member` (every signed-in person) |
| `tenant` | Inside the school the grant belongs to. Blocked while the school is suspended (except support sessions). A branch-limited grant acts only in its branch, but can `:read`/`:view` school-wide | `school_owner`, `school_admin`, `instructor`, `student`, `support_viewer` |
| `platform` | Everywhere | `super_admin` |

Seeded roles ([V1_0_10__SEED_DATA.sql](src/main/resources/db/migration/V1_0_10__SEED_DATA.sql)):

| Role | Kind | Can grant | Branch-limited? |
|---|---|---|---|
| `visitor` | anyone | — | — |
| `member` | every signed-in person | — | — |
| `super_admin` | platform (requires MFA) | `super_admin`, `school_owner` | — |
| `support_viewer` | active support session | — | — |
| `school_owner` | school | `school_owner`, `school_admin`, `instructor`, `student` | no |
| `school_admin` | school | `instructor`, `student` | optional |
| `instructor` | school | — | optional |
| `student` | school | — | optional |

Outcomes: no or invalid token → **401**; authenticated but not allowed → **403**, both in the standard
[error envelope](#errors-and-exceptions). Each `@RequirePermission` denial, and each allowed use of a permission
marked `is_sensitive`, is written to the `access_log` table.

### The current user

```java
@GetMapping("/me/invitations")
public ApiResponse<List<UserRoleResponse>> myInvitations(@AuthenticationPrincipal AuthenticatedUser user) {
    return ApiResponse.success(userRoleService.myInvitations(user.id()));
}
```

[AuthenticatedUser](src/main/java/io/sala/krob_krong/iam/security/AuthenticatedUser.java) has `id()`,
`sessionId()`, `kind()` (`person` or `service`), `email()`, `displayName()`, `mfaVerified()` and `isPerson()`.
Outside controllers, use [Principals](src/main/java/io/sala/krob_krong/iam/security/Principals.java):
`Principals.current()` and `Principals.currentUserId()` (both `Optional`). Never take a user id from a request
parameter to mean "the caller".

### Checks that need request data

When the decision depends on the request body or on data you must load first, check in the service with
`AccessPolicyService` and throw a business error:

```java
if (!accessPolicy.canGrantRole(actorId, request.getRoleId(), schoolId, request.getBranchId())) {
    throw new BusinessException(IAMErrorCode.ROLE_GRANT_NOT_ALLOWED);
}
```

| Method | Use |
|---|---|
| `canAccess(userId, permission, schoolId, branchId, targetUserId)` | Same rules as `@RequirePermission` |
| `canGrantRole(granterId, roleId, schoolId, branchId)` | May this user grant this role here? |
| `effectivePermissions(userId, schoolId, branchId)` | All permission codes the user has in that context |
| `grantableRoleIds(granterId, schoolId, branchId)` | Roles the user may grant there |

Authorization is enforced **only** in the application. The database triggers enforce data rules (valid state
transitions, immutable fields, "a school keeps at least one owner", "nobody grants themselves a role") but not
permissions — so every new write path must be protected by `@RequirePermission` or `AccessPolicyService`.

### Adding a permission

1. Add a Flyway migration that inserts the code into `permission` and maps it in `role_permission` with a scope.
   The `role_permission_guard` trigger rejects scopes that don't fit the role kind (e.g. `tenant` on a platform
   role, or a write permission on `support_viewer`).
2. Use it: `@RequirePermission("your.resource:action")`.
3. Restart. Roles, scopes and grant rules are cached in memory for the life of the process
   ([RoleCatalogProvider](src/main/java/io/sala/krob_krong/iam/security/policy/RoleCatalogProvider.java)).

### Tokens

[TokenService](src/main/java/io/sala/krob_krong/iam/security/TokenService.java) issues HS256 JWT access tokens
(type `at+jwt`, with issuer, audience, subject, session id, kind and MFA claims) and opaque refresh tokens stored
as SHA-256 hashes. A refresh-token *family* is a session:

- `issueRefreshToken(userId, null, userAgent, ip)` starts a session; pass the previous family id to continue it.
- `requireActive(raw)` returns the stored token; presenting an already-rotated token revokes the whole family
  (reuse detection) and fails with `REFRESH_TOKEN_REUSE`.
- `markRotated(previous, newId)` is atomic, so two concurrent refreshes with one token cannot both succeed.
- `revoke(raw)` ends the session.

Each request also checks that the token's session is still active and the account is still active, so logout,
reuse detection and suspension cut off access tokens before they expire.

### Normal-user authentication

These public endpoints use email/password credentials and return the standard `ApiResponse` envelope:

| Method | Path | Body | Success |
|---|---|---|---|
| POST | `/api/auth/register` | `email`, `password`, `display_name` | 201, new account and token pair |
| POST | `/api/auth/login` | `email`, `password` | 200, new session and token pair |
| POST | `/api/auth/refresh` | `refresh_token` | 200, rotated token pair in the same session |

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"person@example.com","password":"a long unique passphrase","display_name":"Normal Person"}'

curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"person@example.com","password":"a long unique passphrase"}'

curl -X POST http://localhost:8080/api/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refresh_token":"<latest refresh token>"}'
```

The `data` object contains `user_id`, `email`, `display_name`, `token_type` (`Bearer`), `access_token`,
`expires_in` (access-token lifetime in seconds), `access_token_expires_at`, `refresh_token`, and
`refresh_token_expires_at`. Auth responses have `Cache-Control: no-store`. Send access tokens in the
`Authorization: Bearer …` header on protected requests; omit that header on these three public requests.
Always replace the stored refresh token with the latest response. Clients must serialize refresh requests:
reusing the previous token, including a concurrent duplicate request, revokes that session and requires login.
A separate login creates an independent session and is unaffected by revocation of another session.

Registration accepts an email (maximum 254 characters), a password of 15–128 characters, and a nonblank
display name (maximum 100 characters). Email matching is case-insensitive; passwords are never trimmed.
Passwords use salted PBKDF2-HMAC-SHA256 (600,000 iterations), stored with an encoder identifier in
`app_user.password_hash`; refresh tokens remain stored only as SHA-256 hashes. Hashing and password
verification happen outside database transactions. Registration and session issuance commit atomically;
refreshes lock the account before reading token state and atomically inserting/rotating tokens.

Every registered account is an active, **unverified person**. Registration never grants a school or platform
role or marks MFA verified. The ordinary `member` role is implicit in the existing authorization policy;
there is no `user_role` insert. Existing accounts without password credentials and service accounts cannot use
password login. Accounts with MFA enabled receive `MFA_REQUIRED`; a separate MFA challenge flow is not
implemented, so these endpoints cannot bypass it.

| Error | HTTP | Meaning |
|---|---|---|
| `BAD_REQUEST` | 400 | Invalid fields; messages are in `error.details.fields`, without rejected secrets |
| `EMAIL_ALREADY_REGISTERED` | 409 | Email already exists, including a concurrent registration conflict |
| `INVALID_CREDENTIALS` | 401 | Unknown account, wrong password, no password credential, or service account |
| `ACCOUNT_SUSPENDED` | 403 | Correct credentials or active refresh token, but inactive account |
| `MFA_REQUIRED` | 403 | This account requires an MFA flow |
| `INVALID_REFRESH_TOKEN` | 401 | Unknown or expired refresh token |
| `REFRESH_TOKEN_REUSE` | 401 | Previously revoked/rotated token; the family is revoked durably |

Email verification is implemented through SMTP; see [School onboarding](docs/onboarding.md).
Password recovery, MFA challenges, and login throttling remain separate work. Configure
rate limits at the deployment boundary before exposing these public credential endpoints to the internet.

## Common utilities

### API responses and paging

JSON endpoints return [ApiResponse](src/main/java/io/sala/krob_krong/common/response/ApiResponse.java):

```java
return ApiResponse.success(dto);                       // HTTP 200, single object or list
return ApiResponse.create(dto);                        // HTTP 201 with the same envelope
return ApiResponse.paginated(PageResponse.from(page));  // Spring Data Page<T> → content + meta
```

```json
{
  "data": { "content": [ … ], "meta": { "total_elements": 42, "total_pages": 3, "page": 0, "size": 20, "has_next": true } },
  "error": null,
  "request_id": "7c9e…",
  "trace_id": null,
  "timestamp": "2026-10-06T00:25:19.849Z"
}
```

`ApiResponse.create(...)` returns `ApiResponse<T>`, like `success(...)`. `ApiResponseStatusAdvice` applies
HTTP 201 from internal metadata that is excluded from JSON; no controller status annotation is needed.

Accept paging with a [PageRequest](src/main/java/io/sala/krob_krong/common/request/PageRequest.java) parameter
(bound from `?page=0&size=20`) and convert it with `pageRequest.toSpring()` or `pageRequest.toSpring(Sort.by("name"))`.

### Errors and exceptions

Throw a business error; [GlobalExceptionHandler](src/main/java/io/sala/krob_krong/common/exceptions/GlobalExceptionHandler.java)
turns it into the envelope with the right status:

```java
throw new BusinessException(IAMErrorCode.ROLE_NOT_FOUND);
throw new BusinessException(IAMErrorCode.USER_NOT_FOUND, "No user with email " + email);
throw new KrobKrongException(CommonErrorCode.BAD_REQUEST, "partCount must be 1-10000").withContext("part_count", n);
```

```json
{ "data": null,
  "error": { "code": "ROLE_GRANT_NOT_ALLOWED", "message": "You may not grant or revoke this role",
             "category": "AUTHORIZATION", "details": null }, … }
```

**Error codes** implement [ErrorCode](src/main/java/io/sala/krob_krong/common/errors/ErrorCode.java)
(`code()`, `httpStatus()`, `defaultMessage()`, `category()`). Give each module its own enum, like
[IAMErrorCode](src/main/java/io/sala/krob_krong/iam/error/IAMErrorCode.java), which derives the category from
the status. Generic codes live in [CommonErrorCode](src/main/java/io/sala/krob_krong/common/errors/CommonErrorCode.java):
`BAD_REQUEST`, `UNAUTHORIZED`, `FORBIDDEN`, `RESOURCE_NOT_FOUND`, `METHOD_NOT_ALLOWED`, `CONFLICT`,
`UNSUPPORTED_MEDIA_TYPE`, `STORAGE_ERROR`, `INTERNAL_ERROR`.

**Translating third-party exceptions.** Implement
[ExceptionMapper](src/main/java/io/sala/krob_krong/common/exceptions/ExceptionMapper.java) as a `@Component`;
the handler tries mappers in `order()` before falling back to `INTERNAL_ERROR`. Return `null` to pass.

```java
@Component
public class PaymentExceptionMapper implements ExceptionMapper<PaymentGatewayException> {
    public Class<PaymentGatewayException> supportedType() { return PaymentGatewayException.class; }
    public KrobKrongException map(PaymentGatewayException e) {
        return new KrobKrongException("PAYMENT_PROVIDER_UNAVAILABLE", "Payment provider unavailable", 502);
    }
}
```

Built-in mappers:

| Mapper | Turns | Into |
|---|---|---|
| `SqlStateExceptionMapper` | Database guard trigger errors and constraint violations | 403 (guard denial), 400 (rule violation, with the trigger's message), 409 (unique/overlap) |
| `S3ExceptionMapper` | S3 / RustFS errors | 404 (unknown key or upload), 400 (bad part/ETag), 502 `STORAGE_ERROR` otherwise |
| `AuthenticationExceptionMapper` | Missing/invalid token | 401 |
| `AccessDeniedExceptionMapper` | `@RequirePermission` denials | 403 (401 if anonymous) |

Engine messages that would leak table or constraint names are replaced with generic text; messages written in
trigger `RAISE` statements are passed through because they are meant for users.

### S3 storage (`S3Util`)

[S3Util](src/main/java/io/sala/krob_krong/common/utils/s3/S3Util.java) wraps one configured bucket and works with
AWS S3 and S3-compatible stores such as RustFS: it uses path-style addresses (`host/bucket/key`), sends checksums
only when an operation requires them, and keeps presigned URLs free of checksum parameters. Inject the interface:

```java
private final S3Util s3Util;
```

| Method | Returns |
|---|---|
| `presignUpload(key, contentType)` | `PresignedUrl` for a single `PUT` |
| `presignDownload(key)` | `PresignedUrl` for a `GET` |
| `startMultipartUpload(key, contentType)` | `MultipartUpload(key, uploadId, partSize)` |
| `presignUploadParts(key, uploadId, partCount)` | one `PresignedPart` per part (1–10,000) |
| `listUploadedParts(key, uploadId)` | parts already stored, for resuming |
| `completeMultipartUpload(key, uploadId, List<PartETag>)` | `StoredObject` |
| `abortMultipartUpload(key, uploadId)` | discards the parts |
| `upload(key, InputStream, contentType)` | server-side chunked upload → `StoredObject` |
| `stat(key)` | `Optional<StoredObject>` (empty if missing) |
| `delete(key)` | — |

**Small files — single presigned upload.** Hand the client the whole `PresignedUrl`; it must send the `headers`
it contains (e.g. `content-type`) exactly as signed:

```java
PresignedUrl url = s3Util.presignUpload("schools/" + schoolId + "/logo.png", "image/png");
// client: PUT url.url() with url.headers(), body = file bytes
```

**Large files — chunked upload with ETags.**

```
server  startMultipartUpload(key, type)            → uploadId, partSize
server  presignUploadParts(key, uploadId, n)       → n URLs
client  PUT each chunk (partSize bytes, last may be smaller) to its URL, keep the response's ETag header
server  completeMultipartUpload(key, uploadId, [PartETag(partNumber, eTag), …])  → StoredObject
        (abortMultipartUpload on cancel; listUploadedParts to resume after a failure)
```

Parts may be sent in any order and completed with ETags in any order; the util sorts them, rejects duplicates and
missing ETags, and storage rejects an ETag that doesn't match what was uploaded (→ 400).

**Server-side upload.** `upload(key, inputStream, contentType)` streams the content in `part-size` chunks: content
smaller than one part goes up as a single `PUT`; larger content becomes a multipart upload where each part is sent
with a `Content-MD5`, each returned ETag is checked against the part's MD5, and the final ETag is checked against
the S3 multipart formula. Any failure aborts the upload. Call it outside transactions.

[ETags](src/main/java/io/sala/krob_krong/common/utils/s3/ETags.java) has the helpers it uses: `unquote`, `quote`,
`md5`, `contentMd5`, `hex` and `multipart(partMd5s)`.

Configuration (`krob-krong.storage.s3.*`):

| Property | Meaning | Default |
|---|---|---|
| `endpoint` | S3 API the server calls | required |
| `public-endpoint` | Address put into presigned URLs, when clients reach storage differently (e.g. `rustfs:9000` inside Docker vs `localhost:9000` in a browser) | `endpoint` |
| `access-key`, `secret-key` | Credentials | required |
| `bucket` | Bucket all keys live in | required |
| `region` | Signing region | `us-east-1` |
| `path-style-access` | `host/bucket/key` URLs (RustFS needs this) | `true` |
| `presign-ttl` | Lifetime of presigned URLs | `15m` |
| `part-size` | Chunk size; at least 5MB (S3 minimum) | `8MB` |
| `auto-create-bucket` | Create the bucket at startup if missing (logs a warning if storage is down) | `false` |

Browser uploads: the browser can read each part's `ETag` only if the bucket's CORS configuration exposes that
header.

### Logging and masking

[MaskedLogger](src/main/java/io/sala/krob_krong/common/log/MaskedLogger.java) wraps SLF4J and masks sensitive
fields before they reach the logs:

```java
private static final MaskedLogger log = MaskedLogger.of(PaymentClient.class);

log.info("Provider replied: {}", log.maskJson(responseBody));
log.info("Headers: {}", log.maskHeaders(headers));
log.info("Body: {}", log.maskBody(body, contentType));   // JSON, XML, or truncated text
```

The underlying [JsonMasker](src/main/java/io/sala/krob_krong/common/utils/JsonMasker.java) and
[XmlMasker](src/main/java/io/sala/krob_krong/common/utils/XmlMasker.java) can be used directly:
`JsonMasker.mask(json, MaskingConfig.defaults())`. The defaults mask `password`, `token`, `secret`,
`authorization`, card numbers, `cvv`, `ssn`, `pin`, API and private keys, and any field whose name matches those
words. Log only DTOs or masked strings — never entities, tokens or raw request bodies.

### Request tracing (`@RequestId`, `@TraceId`)

[TraceContext](src/main/java/io/sala/krob_krong/common/utils/TraceContext.java) defines the headers
`X-Request-ID`, `X-Trace-ID`, `X-Span-ID` and the MDC keys `request_id`, `trace_id`, `span_id`.
[MDCFilter](src/main/java/io/sala/krob_krong/common/trace/MDCFilter.java) reads or generates them per request and
echoes them on the response; `@RequestId String requestId` / `@TraceId String traceId` controller parameters are
meant to receive them, and `ApiResponse` copies them into every body. **These are not wired up yet** — see
[Known gaps](#known-gaps); today `request_id` in responses is a fresh random value.

## Database

Migrations live in [src/main/resources/db/migration](src/main/resources/db/migration), one file per
responsibility:

| Migration | Contains |
|---|---|
| `V1_0_0__CORE_EXTENSIONS_AND_FUNCTIONS` | `btree_gist`, `app_actor()` helpers |
| `V1_0_1__ACCOUNT` | `app_user` |
| `V1_0_2__SCHOOL` | `school`, `school_review` |
| `V1_0_3__BRANCH` | `branch`, `reserved_slug`, `workspace`, `resolve_workspace()` |
| `V1_0_4__MEDIA` | `media_asset`, `media_rendition` |
| `V1_0_5__RBAC` | `permission`, `role`, `role_permission`, `role_grant_rule`, `user_role` |
| `V1_0_6__SUPPORT_AND_AUDIT` | `support_access`, `audit_event`, `audit()`, `access_log` |
| `V1_0_8__WRITE_GUARDS` | Trigger functions enforcing data rules on writes |
| `V1_0_9__REFRESH_TOKEN` | `refresh_token` |
| `V1_0_10__SEED_DATA` | Permissions, roles, scopes, grant rules, reserved slugs |
| `V1_0_11__USER_PASSWORD` | Nullable password credential for person accounts; existing users remain unchanged |
| `V1_0_12__SCHOOL_ONBOARDING` | Email verification tokens, image pipeline service identity, review read permissions and extended review snapshots |

Add changes as new files (`V1_0_13__WHAT_IT_DOES.sql`, …); never edit a migration that has run anywhere.
Entities must match the schema exactly — startup fails otherwise.

**Who did it.** Write guards and the audit trail read the acting user from `app.actor_id`. The transaction manager
sets it automatically at the start of every write transaction from the authenticated user
([ActorBindingJpaDialect](src/main/java/io/sala/krob_krong/iam/security/persistence/ActorBindingJpaDialect.java)),
so guarded tables can only be written by an authenticated request (or code that runs with one). Guard errors come
back as API errors through `SqlStateExceptionMapper`.

**TimescaleDB** is installed in the database (`CREATE EXTENSION` is not needed); it is not used by any table yet.
`audit_event` and `access_log` are natural candidates for hypertables.

## Configuration

All settings are in [application.yaml](src/main/resources/application.yaml) and can be overridden by environment
variables using Spring's relaxed binding (e.g. `KROB_KRONG_SECURITY_JWT_SECRET`,
`KROB_KRONG_STORAGE_S3_SECRET_KEY`, `SPRING_DATASOURCE_URL`).

| Property | Meaning |
|---|---|
| `spring.mail.*` / `krob-krong.account.verification.*` | SMTP delivery and public verification link; [environment variables](docs/onboarding.md#email-delivery) |
| `spring.datasource.*` | PostgreSQL connection; Hikari pool of 20, sessions killed after 30s idle inside a transaction |
| `krob-krong.security.jwt.secret` | HS256 signing key, at least 32 bytes |
| `krob-krong.security.jwt.issuer` / `audience` | Token `iss` / `aud` (`krob-krong` / `krob-krong-api`) |
| `krob-krong.security.jwt.ttl` / `refresh-ttl` / `clock-skew` | Access token lifetime (15m default, 12h in yaml), refresh lifetime (30d), allowed clock skew (30s) |
| `krob-krong.security.cors.allowed-origins` / `max-age` | Browser origins allowed to call the API |
| `krob-krong.storage.s3.*` | See [S3 storage](#s3-storage-s3util) |

> **The JWT secret in `application.yaml` is a development key that is committed to the repository.** Anyone with
> the repository can sign tokens for any user with it. Every shared or production environment must override it,
> e.g. `KROB_KRONG_SECURITY_JWT_SECRET=$(openssl rand -base64 48)`.

## API overview

All under `/api`. Authentication and email confirmation are public. Approved school profiles/branding and live
workspace resolution are public; other operations require a bearer token.
See [School onboarding](docs/onboarding.md) for the complete new endpoint list, lifecycle rules and SMTP setup.
The [bruno](bruno) folder is a [Bruno](https://www.usebruno.com/) collection (3.x, YAML format) with requests,
docs and tests. Open it in Bruno, choose **Local**, set `auth_email`, `auth_display_name` and the `auth_password`
secret, then run **Authentication → Register** or **Login**. Successful authentication requests store both
tokens as in-memory runtime variables for subsequent requests; **Refresh token** replaces both.

| Method | Path | Authorization |
|---|---|---|
| POST | `/auth/register` | public; creates ordinary person account |
| POST | `/auth/login` | public; email/password |
| POST | `/auth/refresh` | public; latest refresh token |
| GET | `/roles?kind=` | signed in |
| GET | `/roles/{role_id}` | signed in |
| GET | `/roles/grantable?school_id=&branch_id=` | signed in; roles the caller may grant there |
| GET | `/permissions?search=` | signed in |
| GET | `/permissions/{code}` | signed in |
| GET | `/permissions/effective?school_id=&branch_id=` | signed in; the caller's own permissions |
| POST | `/schools/{school_id}/members/invite` | grant rules (`canGrantRole`) |
| GET | `/schools/{school_id}/members` | `member:read` in the school |
| GET | `/schools/{school_id}/members/{user_id}/roles` | `member:read` in the school, or yourself |
| GET | `/me/invitations` | signed in |
| POST | `/me/invitations/{user_role_id}/accept` | the invited person only |
| DELETE | `/user-roles/{user_role_id}` | the grant holder, or someone who may grant that role |

## Testing

```bash
docker compose up -d --wait     # the context test connects to PostgreSQL
./gradlew test                  # unit, slice and context tests
./gradlew build                 # tests + spotlessCheck
S3_IT=true ./gradlew test --tests '*S3UtilRustfsIT'   # live S3 tests against RustFS
# Create a disposable PostgreSQL database first; the auth suite migrates it and leaves test records.
AUTH_IT=true AUTH_TEST_DB_URL=jdbc:postgresql://localhost:5432/krob_krong_auth_test \
  ./gradlew test --tests '*AuthPostgresIT' --rerun-tasks
```

| Test | Covers |
|---|---|
| `AuthControllerTest` / `AuthServiceImplTest` | Public routes, validation, credential checks and person-only registration |
| `AuthPostgresIT` | Real migrations, HTTP auth flow, hashing, privilege isolation, token reuse, concurrent refresh/registration, transaction rollback |
| `AccessPolicyRulesTest` | The RBAC rules against a copy of the seeded roles (`SeedRoles` fixture) |
| `AccessPolicyServiceImplTest` | Loading grants, branch status, support sessions (mocked repositories) |
| `UserRoleControllerSecurityTest` | 401/403 envelopes, token → principal, `@RequirePermission`, access log |
| `JwtTokenRoundTripTest` | Token signing, audience/type/expiry checks, session revocation |
| `ActorBindingJpaDialectTest` | `app.actor_id` binding per write transaction |
| `SqlStateExceptionMapperTest` | Database error → API error mapping |
| `SchoolOnboardingIT` / `SchoolImageProcessorTest` | Real PostgreSQL/SMTP/RustFS lifecycle, verification and review guards, image processing; [run instructions](docs/onboarding.md#schema-and-verification) |
| `S3UtilImplTest` / `S3UtilRustfsIT` | Chunking, ETag checks and presigning (mocked / live RustFS) |
| `TransactionBoundaryTest` | No class-level or controller `@Transactional`; refresh-token revocation commits independently |

Patterns worth copying:

- **Security slice test:** `@WebMvcTest(MyController.class)` plus
  `@Import({SecurityConfig.class, RbacAuthorizer.class, UserJwtAuthenticationConverter.class,
  RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, AuthenticationExceptionMapper.class,
  AccessDeniedExceptionMapper.class})`, `@MockitoBean` for `AccessPolicyService`, `AccessLogService`, `JwtDecoder`
  and your service, and `with(authentication(new UserAuthenticationToken(jwt, user, List.of())))` to act as a user.
- **Mocking AWS SDK clients:** `mock(S3Client.class, withSettings().defaultAnswer(CALLS_REAL_METHODS))` with
  `doReturn/doAnswer` on the request-object overloads, so the SDK's lambda-builder overloads still work.

## Recipe: a new protected endpoint

Say branch admins should list a branch's workspaces.

1. **Permission** — if a fitting one doesn't exist, add it in a migration (`permission` + `role_permission`).
2. **Specification** — `WorkspaceSpecification.byBranchId(branchId).and(WorkspaceSpecification.current())`.
3. **Service** — `WorkspaceService.listForBranch(...)` interface + `WorkspaceServiceImpl`; no `@Transactional`
   for a single read; map entities to DTOs with a MapStruct mapper.
4. **Controller**:

   ```java
   @GetMapping("/schools/{school_id}/branches/{branch_id}/workspaces")
   @RequirePermission(value = "branch:read", school = "#schoolId", branch = "#branchId")
   public ApiResponse<List<WorkspaceResponse>> list(@PathVariable("school_id") UUID schoolId, @PathVariable("branch_id") UUID branchId) {
       return ApiResponse.success(workspaceService.listForBranch(schoolId, branchId));
   }
   ```

5. **Errors** — throw `BusinessException` with a code from the module's error enum; never return `null` for "not found".
6. **Test** — a slice test for 401/403/200 like `UserRoleControllerSecurityTest`.
7. `./gradlew spotlessApply build`.

## Known gaps

- **Authentication follow-ups:** password recovery, MFA challenges, and request throttling
  are not implemented. Existing accounts without a password need a separate credential-provisioning flow.
- **Tracing and request logging are not wired.** Nothing registers `MDCFilter`, `RequestResponseLogFilter` or
  `TraceIdMethodArgumentResolver`, and the resolver checks method annotations instead of parameter annotations, so
  `@RequestId`/`@TraceId` never resolve.
- **`mask:` in `application.yaml` is not applied.** `MaskingConfig` is never registered as configuration
  properties, and the yaml's `mask-pattern` key does not match its `fullMask` field; `MaskedLogger.of(...)` uses
  the built-in defaults.
- **Page size is not capped when bound from the query string.** `PageRequest`'s setters skip the 1–100 check that
  its constructor does.
- **`GET /schools/{school_id}/members` pages over grants, not people**, so a member with several roles counts
  several times toward the page size and total.
- **Valkey runs locally but the app has no client for it yet.**
