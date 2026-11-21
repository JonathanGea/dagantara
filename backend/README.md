# Dagantara — Spring Boot Foundation

Prasyarat menjalankan proyek adalah JDK 25 (LTS). Proyek menyertakan Maven Wrapper yang menjalankan Maven 3.9.9, sehingga instalasi Maven terpisah tidak diperlukan.

Pastikan `JAVA_HOME` menunjuk ke JDK 25. Jika `java -version` masih menunjukkan versi lain di PowerShell, dahulukan JDK dari `JAVA_HOME` untuk sesi terminal tersebut:

```powershell
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

## Scope

Included: OpenAPI/Swagger, success response contract and paged metadata, ProblemDetail exception handling with stable error codes, Jakarta request validation, PostgreSQL/JPA, dev/staging/prod profiles, structured request ID in logs, Spring Security, JWT access tokens and a temporary technical bootstrap login, CORS, pageable/sort allowlist helper, timestamp auditing, Actuator health probes, /api/v1 routing convention, mapper interface, UUID base entity, @Transactional infrastructure, and a multi-stage non-root Dockerfile.

Excluded by decision: database migration tooling, test harness, Redis/cache, rate limiting, file storage, background jobs, multi-tenant context and RBAC. Existing domain specification and agent skill documents remain unchanged and apply to later feature work.

## Development

1. Install JDK 21, Maven 3.6.3+, and PostgreSQL.
2. Create an empty PostgreSQL database named dagantara and grant your local database user access.
3. Copy `.env.example` to `.env`. Set `DB_PASSWORD` and `APP_JWT_SECRET_BASE64`. The `dev` profile defaults to PostgreSQL at `127.0.0.1:5433` with user `dagantara_app`. Set `DB_URL` and `DB_USERNAME` if your local instance differs. You can also set the bootstrap username and BCrypt hash. Export the values before launching Spring Boot.
4. Generate a signing secret: `openssl rand -base64 48`. Do not reuse one secret across environments.
5. To generate a BCrypt hash on a machine with Apache htpasswd installed: `htpasswd -nBC 12 bootstrap`. Enter the password interactively, and copy only the hash portion following `bootstrap:`. Put the hash in single quotes in the shell/.env so dollar signs remain literal.
6. From `backend/`, export environment values and run:

```bash
cd backend
./mvnw spring-boot:run
```

Di PowerShell, gunakan `cd backend` lalu `.\mvnw.cmd spring-boot:run`.

Cek aplikasi di terminal lain.

```bash
docker build -t dagantara-backend .
docker run --rm --env-file .env -p 8080:8080 dagantara-backend
```

For Docker, `DB_URL` must point to a PostgreSQL host reachable from the container (not the container's own localhost). Secrets should be injected through a secrets manager or environment configuration, not baked into the image.

## Verification

Untuk membuat JAR, jalankan `./mvnw clean package` dari folder `backend` (atau `.\mvnw.cmd clean package` di PowerShell).

## User Management

Terapkan skema PostgreSQL 15+ satu kali pada database target sebelum menggunakan akun
tersimpan. Gunakan kredensial PostgreSQL dari lingkungan:

```powershell
psql -X -v ON_ERROR_STOP=1 -f src/main/resources/db/user-management.sql
```

- `POST /api/v1/auth/login` menerima `username` dan `password`. Sertakan
  `tenantCode` untuk akun tenant. Username unik dalam tenant atau di antara akun
  platform. Akun yang bisa login berstatus `ACTIVE`.
- `POST /api/v1/users` menerima `username`, `email`, `fullName`, `password`,
  dan `roleId`. JWT akun tersimpan dan permission efektif `users.create`
  diperlukan. Tenant dan tipe akun mengikuti pembuat, dan role target harus
  memiliki tenant serta scope yang sesuai (`TENANT` atau `PLATFORM`).
  Password dibatasi hingga 72 byte UTF-8. Respons `201` berisi ID pengguna,
  ID tenant, ID role, dan username.
- Kredensial bootstrap teknis tetap dapat digunakan untuk login. Permission
  pembuatan pengguna diberikan melalui role akun yang tersimpan.

Provisioning tenant dan akun pengelola pertama serta kebijakan pengelolaan role
masih menunggu keputusan dalam [spesifikasi User Management](../docs/user-management.md).

Untuk memverifikasi alur yang tersedia, gunakan database PostgreSQL uji kosong
dan set `UM_TEST_PASSWORD` ke password sementara alfanumerik di proses lokal.
Arahkan `PGHOST`, `PGPORT`, `PGDATABASE`, dan `PGUSER` ke database uji.
Terapkan skema dari root repo, muat fixture, lalu jalankan backend dengan koneksi ke
database uji yang sama. Fixture hanya untuk pengujian.

```powershell
psql -X -v ON_ERROR_STOP=1 -f backend/src/main/resources/db/user-management.sql
psql -X -v ON_ERROR_STOP=1 -f bruno/user-management/fixtures.sql
cd bruno
bru run smoke --env Local
bru run user-management -r --env Local --bail
psql -X -v ON_ERROR_STOP=1 -f user-management/verify.sql
```

Buat ulang database uji sebelum mengulangi rangkaian penuh.
