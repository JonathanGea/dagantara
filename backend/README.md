# Dagantara — Spring Boot Foundation

Prasyarat menjalankan proyek adalah JDK 25 (LTS). Proyek menyertakan Maven Wrapper yang menjalankan Maven 3.9.9, sehingga instalasi Maven terpisah tidak diperlukan.

Pastikan `JAVA_HOME` menunjuk ke JDK 25. Jika `java -version` masih menunjukkan versi lain di PowerShell, dahulukan JDK dari `JAVA_HOME` untuk sesi terminal tersebut:

```powershell
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

## Scope

Included: OpenAPI/Swagger, success response contract and paged metadata, ProblemDetail exception handling with stable error codes, Jakarta request validation, PostgreSQL/JPA, dev/staging/prod profiles, structured request ID in logs, Spring Security, JWT access tokens and persisted platform login, CORS, pageable/sort allowlist helper, timestamp auditing, Actuator health probes, /api/v1 routing convention, mapper interface, UUID base entity, @Transactional infrastructure, and a multi-stage non-root Dockerfile.

Excluded by decision: database migration tooling, test harness, Redis/cache, rate limiting, file storage, background jobs, multi-tenant context and RBAC. Existing domain specification and agent skill documents remain unchanged and apply to later feature work.

## Development

1. Install JDK 25 and PostgreSQL 15 or later.
2. Create an empty PostgreSQL database named dagantara, apply the platform account schema below, and grant your local database user access.
3. Copy `.env.example` to `.env`. Set `DB_PASSWORD` and `APP_JWT_SECRET_BASE64`. The `dev` profile defaults to PostgreSQL at `127.0.0.1:5433` with user `dagantara_app`. Set `DB_URL` and `DB_USERNAME` if your local instance differs. Export the values before launching Spring Boot.
4. Generate a signing secret: `openssl rand -base64 48`. Do not reuse one secret across environments.
5. For first-time Admin Platform setup, set the two bootstrap variables described below.
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

## Setup Admin Platform pertama (UM-07)

Jalankan migrasi sekali pada database kosong dari folder `backend/`. Setel host, port, nama database, dan user PostgreSQL ke instance tujuan.

```powershell
psql -X -v ON_ERROR_STOP=1 -f src/main/resources/db/01-platform-accounts.sql
```

Untuk setup akun pertama, ekspor `APP_BOOTSTRAP_USERNAME` dan `APP_BOOTSTRAP_PASSWORD_HASH` sebelum startup. Buat hash dengan `htpasswd -nBC 12 bootstrap` dan salin bagian setelah `bootstrap:`. Gunakan kutip tunggal saat memasukkan hash ke shell PowerShell supaya karakter dolar tetap literal. Backend menyimpan akun dengan role `SUPERADMIN`. Startup ulang mempertahankan akun dan password yang sudah tersimpan, termasuk saat nilai konfigurasi setup berubah. Lepas kedua variabel setelah setup berhasil.

Kirim JSON dengan `username` dan `password` ke `POST /api/v1/auth/login` di akses platform. Respons berisi bearer token dengan ID akun sebagai subject JWT. Perubahan dari login teknis sebelumnya memerlukan login ulang.

Skenario Bruno UM-AT-06 menggunakan akun fixture pada database terisolasi. Ekspor `DAGANTARA_TEST_ADMIN_USERNAME` dan `DAGANTARA_TEST_ADMIN_PASSWORD` untuk fixture tersebut, lalu jalankan dari folder `bruno/`.

```powershell
bru run smoke --env Local
bru run user-management/UM-AT-06-bootstrap-platform-admin --env Local
```

## Verification

Untuk membuat JAR, jalankan `./mvnw clean package` dari folder `backend` (atau `.\mvnw.cmd clean package` di PowerShell).
