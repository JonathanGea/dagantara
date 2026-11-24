# Dagantara — Spring Boot Foundation

Prasyarat menjalankan proyek adalah JDK 25 (LTS). Proyek menyertakan Maven Wrapper yang menjalankan Maven 3.9.9, sehingga instalasi Maven terpisah tidak diperlukan.

Pastikan `JAVA_HOME` menunjuk ke JDK 25. Jika `java -version` masih menunjukkan versi lain di PowerShell, dahulukan JDK dari `JAVA_HOME` untuk sesi terminal tersebut:

```powershell
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

## Scope

Included: OpenAPI/Swagger, success response contract and paged metadata, ProblemDetail exception handling with stable error codes, Jakarta request validation, PostgreSQL/JPA, dev/staging/prod profiles, structured request ID in logs, Spring Security, JWT access tokens, platform and tenant user management with RBAC, CORS, pageable/sort allowlist helper, timestamp auditing, Actuator health probes, /api/v1 routing convention, mapper interface, UUID base entity, @Transactional infrastructure, and a multi-stage non-root Dockerfile.

Excluded by decision: automated database migration tooling, Redis/cache, rate limiting, file storage, and background jobs.

## Development

1. Install JDK 25 and PostgreSQL 15 or later.
2. Create an empty PostgreSQL database named dagantara, apply the User Management, Product, and Inventory migrations below, and grant your local database user access.
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

## Deploy backend ke Railway

### Pilih environment dan profil Spring

Pilih environment dari dropdown proyek Railway sebelum membuka service backend. Environment tersebut menentukan kumpulan service dan variabel yang digunakan, termasuk database. Pilih branch Git yang dideploy pada **Settings → Source** untuk service di environment tersebut. [Environment Railway](https://docs.railway.com/environments) · [Service source](https://docs.railway.com/services#service-source)

Pada tab **Variables** service backend, atur `SPRING_PROFILES_ACTIVE` sesuai profil Spring yang diinginkan.

| Nilai | File konfigurasi Spring | Penggunaan |
| --- | --- | --- |
| `dev` | `application-dev.yml` | Pengembangan lokal dengan logging DEBUG dan Swagger aktif. |
| `staging` | `application-staging.yml` | Deployment uji dengan konfigurasi database staging. |
| `prod` | `application-prod.yml` | Deployment produksi dengan konfigurasi database produksi. |

Nama environment Railway dan nama branch Git tidak mengatur profil Spring secara otomatis. Misalnya, untuk menguji branch `dev` di environment Railway `staging`, set `SPRING_PROFILES_ACTIVE` ke `staging` pada service backend di environment itu.

### Isi variabel service backend

Salin [railway.env.example.json](railway.env.example.json) ke **Variables → Raw Editor** pada service backend. Contoh memakai profil `prod` dan service PostgreSQL bernama `Postgres` di environment yang sama. Sesuaikan prefix `Postgres` jika nama servicenya berbeda. [Variabel Railway](https://docs.railway.com/variables) · [PostgreSQL di Railway](https://docs.railway.com/databases/postgresql)

```json
{
  "SPRING_PROFILES_ACTIVE": "prod",
  "DB_URL": "jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}",
  "DB_USERNAME": "${{Postgres.PGUSER}}",
  "DB_PASSWORD": "${{Postgres.PGPASSWORD}}",
  "APP_PLATFORM_HOST": "${{RAILWAY_PUBLIC_DOMAIN}}",
  "APP_JWT_SECRET_BASE64": "",
  "APP_CORS_ALLOWED_ORIGINS": "",
  "APP_BOOTSTRAP_USERNAME": "",
  "APP_BOOTSTRAP_PASSWORD_HASH": ""
}
```

- Isi `APP_JWT_SECRET_BASE64` dengan hasil `openssl rand -base64 48`. Simpan nilainya di Railway.
- Buat domain publik untuk service backend agar `RAILWAY_PUBLIC_DOMAIN` tersedia. `APP_PLATFORM_HOST` berisi hostname yang dipakai untuk akses platform. Isi `APP_CORS_ALLOWED_ORIGINS` dengan origin frontend jika berbeda dari origin backend. Spring Boot membaca `PORT` yang disediakan Railway. [Referensi variabel Railway](https://docs.railway.com/variables/reference) · [Port dan health check](https://docs.railway.com/deployments/healthchecks)
- Isi kedua variabel `APP_BOOTSTRAP_*` saat membuat Admin Platform pertama. Gunakan username dan hash BCrypt cost 12. Setelah akun dibuat, hapus kedua variabel dari Railway. Ikuti [panduan setup admin](#setup-admin-platform-pertama-um-07).
- Terapkan [migrasi PostgreSQL 01–05](#setup-admin-platform-pertama-um-07) yang belum dijalankan sebelum startup. Jika menggunakan `backend/Dockerfile`, sesuaikan tahap build dan runtime ke Java 25. Proyek menargetkan Java 25 dan Dockerfile saat ini masih menggunakan Java 21.

## Setup Admin Platform pertama (UM-07)

Jalankan migrasi User Management berikut berurutan dari folder `backend/` sebelum startup. Untuk database yang sudah memiliki sebagian skema, mulai dari migrasi pertama yang belum diterapkan. Setel host, port, nama database, dan user PostgreSQL ke instance tujuan.

```powershell
psql -X -v ON_ERROR_STOP=1 -f src/main/resources/db/01-platform-accounts.sql
psql -X -v ON_ERROR_STOP=1 -f src/main/resources/db/02-tenant-onboarding.sql
psql -X -v ON_ERROR_STOP=1 -f src/main/resources/db/03-platform-operators.sql
psql -X -v ON_ERROR_STOP=1 -f src/main/resources/db/04-tenant-accounts.sql
psql -X -v ON_ERROR_STOP=1 -f src/main/resources/db/05-help-center-recovery.sql
```

Untuk setup akun pertama, ekspor `APP_BOOTSTRAP_USERNAME` dan `APP_BOOTSTRAP_PASSWORD_HASH` sebelum startup. Buat hash dengan `htpasswd -nBC 12 bootstrap` dan salin bagian setelah `bootstrap:`. Gunakan kutip tunggal saat memasukkan hash ke shell PowerShell supaya karakter dolar tetap literal. Backend menyimpan akun dengan role `SUPERADMIN`. Startup ulang mempertahankan akun dan password yang sudah tersimpan, termasuk saat nilai konfigurasi setup berubah. Lepas kedua variabel setelah setup berhasil.

Kirim JSON dengan `username` dan `password` ke `POST /api/v1/auth/login` di akses platform. Respons berisi bearer token dengan ID akun sebagai subject JWT dan status `passwordChangeRequired`. Perubahan dari login teknis sebelumnya memerlukan login ulang.

Skenario Bruno UM-AT-06 menggunakan akun fixture pada database terisolasi. Ekspor `DAGANTARA_TEST_ADMIN_USERNAME` dan `DAGANTARA_TEST_ADMIN_PASSWORD` untuk fixture tersebut, lalu jalankan dari folder `bruno/`.

```powershell
bru run smoke --env Local
bru run user-management/UM-AT-06-bootstrap-platform-admin --env Local
```

## Pengajuan dan onboarding tenant (UM-02, UM-06, UM-08, UM-11)

Setel `APP_PLATFORM_HOST` ke hostname akses Dagantara, tanpa skema, port, atau jalur. Nilai lokal bawaan adalah `localhost`. Proxy harus meneruskan hostname dan jalur URL asli yang telah divalidasi. Backend menentukan konteks berdasarkan hostname request dan jalur toko. Header `Forwarded` dan `X-Forwarded-Host` tidak dipakai untuk menentukan konteks tersebut.

| Operasi | Jalur dan input |
| --- | --- |
| Ajukan tenant dari host Dagantara | `POST /api/v1/tenant-applications` dengan `storeName`, `phoneNumber`, dan `proposedDomain`. Respons memuat ID pengajuan dan status `IN_REVIEW`. |
| Tinjau pengajuan | `GET /api/v1/tenant-applications` atau `GET /api/v1/tenant-applications/{id}`. Gunakan `page` dan `size` untuk daftar. |
| Ubah usulan atau tutup pengajuan | `PATCH /api/v1/tenant-applications/{id}` dengan `proposedDomain`, atau `POST /api/v1/tenant-applications/{id}/close`. |
| Selesaikan pengajuan | `POST /api/v1/tenant-applications/{id}/provision` dengan `ownerUsername`, `loginUrl`, dan `loginUrlReady`. Data toko serta nomor HP berasal dari pengajuan. |
| Mulai onboarding langsung | `POST /api/v1/tenants` dengan `storeName`, `phoneNumber`, `ownerUsername`, `loginUrl`, dan `loginUrlReady`. |
| Login dan ganti password sementara | `POST /api/v1/auth/login` dengan `username` dan `password`, lalu `POST /api/v1/auth/password` dengan bearer token dan `newPassword`. Pada URL bersama, awali kedua jalur dengan `/{shopSlug}`. |

Operator membutuhkan permission `tenants.provision` yang tersedia pada role bawaan `SUPERADMIN`. Client mengirim `loginUrlReady: true` setelah operator memastikan URL telah siap. Alamat domain toko menggunakan jalur root. Alamat di host Dagantara menggunakan satu slug toko berhuruf kecil, angka, atau tanda hubung. Hostname dan slug menjadi kunci pemetaan, termasuk jika URL ditulis dengan huruf besar pada bagian hostname atau garis miring penutup.

Respons provisioning sukses memuat `tenantId`, `ownerId`, `loginUrl`, `ownerUsername`, dan `temporaryPassword`. Operator memberikan kredensial itu kepada pemilik. Setelah login sementara, gunakan token hanya pada endpoint ganti password di URL toko yang sama. Setelah perubahan berhasil, lakukan login ulang dengan password baru.

Untuk memverifikasi onboarding, jalankan Bruno di database fixture terisolasi. Gunakan akun platform berizin dan satu akun platform dengan role tanpa `tenants.provision`. Ekspor kredensial sebagai `DAGANTARA_TEST_ADMIN_USERNAME`, `DAGANTARA_TEST_ADMIN_PASSWORD`, `DAGANTARA_TEST_LIMITED_USERNAME`, dan `DAGANTARA_TEST_LIMITED_PASSWORD`. Setel `DAGANTARA_TEST_PLATFORM_HOST` sesuai hostname pada `baseUrl` dan `APP_PLATFORM_HOST`.

```powershell
bru run smoke --env Local
bru run user-management/UM-AT-07-tenant-provisioning --env Local --bail
bru run user-management/UM-AT-13-closed-application --env Local --bail
bru run user-management/UM-AT-09-tenant-login-and-password --env Local --bail
```

## Pengelolaan akun dan role (UM-01–UM-05, UM-10, UM-11)

| Cakupan | Akun | Role |
| --- | --- | --- |
| Platform | `/api/v1/users` | `/api/v1/roles` |
| Tenant | `/api/v1/tenant/users` | `/api/v1/tenant/roles` |

Gunakan akses platform untuk rute platform dan URL toko untuk rute tenant. Pada URL bersama, tambahkan awalan `/{shopSlug}` ke rute tenant. Pembuatan akun dengan role awal memerlukan `users.create` dan `users.role.assign`. Kredensial awal yang dikembalikan merupakan password sementara. Setelah penetapan role atau perubahan permission, request berikutnya memakai izin efektif terbaru.

Pada kedua cakupan, gunakan `PUT /{id}/role` pada rute akun untuk mengganti role dan `PUT /{id}/permissions` pada rute role untuk mengganti permission. Gunakan `POST /{id}/reset-password` dan `POST /{id}/deactivate` untuk reset dan penonaktifan. Aktivasi ulang menggunakan `POST /{id}/reactivate` di platform dan `POST /{id}/activate` di tenant. `DELETE /{id}` mengarsipkan akun dan mempertahankan riwayatnya.

## Pemulihan admin tenant (UM-09)

Operator Help Center masuk melalui akses platform dengan permission `tenants.admins.password.reset`. Alur ini menggunakan lookup admin dan konfirmasi cara operator memeriksa pemohon. Reset menghasilkan password sementara, mencabut sesi lama, dan mencatat operator, target, metode verifikasi, serta waktu. Ikuti kontrak pada [tiket 05](../docs/tickets/user-management/05-pemulihan-help-center.md#alur-help-center).

## Migrasi Product dan Inventory Management

Terapkan migrasi berikut dari folder `backend/` setelah migrasi User Management 01–05. Pada database yang sudah memiliki sebagian skema, mulai dari migrasi pertama yang belum diterapkan.

```powershell
psql -X -v ON_ERROR_STOP=1 -f src/main/resources/db/06-product-catalog.sql
psql -X -v ON_ERROR_STOP=1 -f src/main/resources/db/07-product-media.sql
psql -X -v ON_ERROR_STOP=1 -f src/main/resources/db/08-inventory-receipts.sql
psql -X -v ON_ERROR_STOP=1 -f src/main/resources/db/09-inventory-corrections.sql
```

Migrasi mendaftarkan izin katalog dan Inventory serta memberikannya kepada role Admin Tenant yang sudah ada. Provisioning tenant baru memakai katalog permission yang sama. Ikuti [Product Management](../docs/specs/product-management.md) dan [Inventory Management](../docs/specs/inventory-management.md) untuk kontrak API dan skenario Bruno.

## Verification

Untuk membuat JAR, jalankan `./mvnw clean package` dari folder `backend` (atau `.\mvnw.cmd clean package` di PowerShell).

Untuk regresi gabungan User Management, pakai database fixture terisolasi dengan migrasi 01–05. Selain fixture onboarding, atur `DAGANTARA_TEST_PLATFORM_ROLE_ID` ke ID role `SUPERADMIN`. Jalankan `bruno/user-management/UM-AT-05-platform-scope/fixture.sql` dengan `psql -X -v ON_ERROR_STOP=1` sebelum tes.

Jalankan perintah berikut dari `bruno/`. Sesuaikan `baseUrl` di environment `Local` dengan backend uji jika memakai port lain.

```powershell
bru run smoke --env Local --bail --noproxy --sandbox developer
bru run user-management -r --env Local --bail --noproxy --sandbox developer
```

Setelah rangkaian HTTP selesai, jalankan `user-management/verify-platform-operators.sql` dan `user-management/05-help-center-recovery/verify-audit.sql` dengan `psql -X -v ON_ERROR_STOP=1` pada database fixture yang sama.
