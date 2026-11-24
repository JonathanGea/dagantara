# 05 — Pemulihan admin tenant melalui Help Center

[Spesifikasi User Management](../../specs/user-management.md)

Status: done

## What to Build

Operator Help Center memulihkan password Pemilik Tenant atau Admin Tenant tambahan setelah memeriksa pemohon. Batasi akses operator pada informasi yang diperlukan untuk pemulihan dan simpan audit tindakan. Cakupan meliputi UM-09 dan bagian kredensial dari UM-02 dan UM-05.

## Acceptance Criteria

- [x] Operator dengan izin pemulihan dapat memeriksa informasi yang diperlukan dan mencatat bahwa identitas admin telah diperiksa. Operator dengan izin provisioning saja dan permintaan tanpa verifikasi ditolak tanpa perubahan akun (UM-AT-08, UM-AT-10).
- [x] Reset Pemilik Tenant maupun Admin Tenant tambahan mencabut password dan sesi lama. Password sementara hanya membuka alur ganti password (UM-AT-10).
- [x] Audit mencatat operator, akun target, dan waktu reset. Kredensial tetap di luar audit dan log. Informasi pemulihan tersedia melalui Help Center (UM-AT-10).

## Alur Help Center

Jalankan migrasi `backend/src/main/resources/db/05-help-center-recovery.sql` setelah migrasi 01–04. Operator masuk di akses platform dengan permission `tenants.admins.password.reset`.

1. Kirim `loginUrl` toko dan `username` admin ke `POST /api/v1/help-center/tenant-admins/lookup`. Untuk admin aktif dengan nomor pemulihan, respons mengembalikan `tenantId`, `accountId`, dan `phoneNumber` yang tercatat.
2. Periksa identitas pemohon dengan mencocokkan nomor masuk, menelepon balik nomor tercatat, atau mengonfirmasi langsung jika mengenal pemohon.
3. Setelah pemeriksaan, kirim `verificationMethod` ke `POST /api/v1/help-center/tenants/{tenantId}/admins/{accountId}/reset-password` dengan nilai `MATCHED_INCOMING_NUMBER`, `CALLBACK_TO_REGISTERED_NUMBER`, atau `KNOWN_REQUESTER` sesuai tindakan operator. Berikan `temporaryPassword` dari respons kepada pemohon yang telah diperiksa. Pemohon masuk dan mengganti password di URL toko.

## Verification

Build, smoke, skenario Help Center (UM-AT-08 dan UM-AT-10), regresi delegasi (UM-AT-08), regresi akun tenant (119 dari 119 request), dan verifikasi audit PostgreSQL lulus. Pengujian memakai backend pada port 18085 dan PostgreSQL 16 terisolasi pada port 55385 dengan migrasi 01–05. Windows mencadangkan port 55485.

Jalankan build dari folder `backend/` dengan `.\mvnw.cmd clean package`. Dari `bruno/`, ekspor `DAGANTARA_TEST_ADMIN_USERNAME` dan `DAGANTARA_TEST_ADMIN_PASSWORD` milik fixture platform, lalu jalankan perintah berikut. Regresi akun tenant juga membutuhkan `DAGANTARA_TEST_PLATFORM_ROLE_ID` milik role `SUPERADMIN`. Pengujian ini memakai Bruno CLI 4.3.0 melalui `npx.cmd --offline @usebruno/cli`.

```powershell
bru run smoke --env Local --env-var baseUrl=http://localhost:18085 --bail --noproxy --sandbox developer
bru run user-management/05-help-center-recovery --env Local --env-var baseUrl=http://localhost:18085 --bail --noproxy --sandbox developer
bru run user-management/UM-AT-08-platform-delegation-limits --env Local --env-var baseUrl=http://localhost:18085 --bail --noproxy --sandbox developer
bru run user-management/04-tenant-accounts-and-roles --env Local --env-var baseUrl=http://localhost:18085 --bail --noproxy --sandbox developer
psql -X -h 127.0.0.1 -p 55385 -U dagantara_app -d dagantara -v ON_ERROR_STOP=1 -f user-management/05-help-center-recovery/verify-audit.sql
```

Pemeriksaan SQL menggunakan database uji yang sama. Skrip memeriksa tepat satu audit per target yang dipulihkan, operator dan waktu reset, kolom yang disimpan, serta versi sesi dan hash akhir kedua target.

## Blocked By

- [Tiket 02](02-onboarding-tenant.md) untuk akun pemilik.
- [Tiket 03](03-operator-platform.md) untuk role operator Help Center.
- [Tiket 04](04-akun-dan-role-tenant.md) untuk Admin Tenant tambahan.
