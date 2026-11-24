# 04 — Akun dan role tenant

[Spesifikasi User Management](../../specs/user-management.md)

Status: done

## What to Build

Pemilik dan pengelola tenant mengelola akun dan role sesuai izin. Batasi setiap tindakan pada tenant terautentikasi dan tegakkan perlindungan Pemilik Tenant dan Admin Tenant. Cakupan meliputi bagian tenant dari UM-01 hingga UM-04, UM-06, UM-10, dan UM-11.

## Acceptance Criteria

- [x] Pengelola tenant dapat membuat akun dalam tenant sendiri dengan satu role yang sah dan password sementara. Operasi tanpa autentikasi, tanpa izin, atau dengan role dari tenant atau cakupan lain ditolak tanpa mutasi (UM-AT-01, UM-AT-02, UM-AT-05).
- [x] Pembuatan role, perubahan permission, dan penetapan role diperiksa terpisah. Pengelola tidak dapat mendelegasikan atau mengubah izin di luar kewenangannya. Perubahan berlaku pada aksi berikutnya (UM-AT-03, UM-AT-08).
- [x] Baca, tulis, lookup relasi, pagination, dan total terbatas pada tenant akun. Lookup ID dari tenant lain memberi `404`. Pengelola dengan izin yang sama berhasil mengakses data yang sah (UM-AT-04).
- [x] Siklus akun staf menerapkan izin per tindakan. Reset, penonaktifan, dan pengarsipan mencabut sesi sebelumnya. Pengarsipan menyimpan riwayat dan mencegah aktivasi biasa (UM-AT-11).
- [x] Hanya Pemilik Tenant yang dapat memberi atau mencabut role `TENANT_ADMIN`. Penunjukan admin tambahan mencatat nomor HP. Pengelolaan staf biasa ditolak untuk Admin Tenant, dan aksi perubahan role, penonaktifan, serta pengarsipan ditolak untuk Pemilik Tenant (UM-AT-12).

## Verification

Suite Bruno tiket 04 lulus 119 dari 119 request pada PostgreSQL terisolasi dengan migrasi 01–04. Jalankan dari `bruno/` dengan environment `Local` dan setel `baseUrl` ke backend uji. Fixture memerlukan `DAGANTARA_TEST_ADMIN_USERNAME`, `DAGANTARA_TEST_ADMIN_PASSWORD`, dan `DAGANTARA_TEST_PLATFORM_ROLE_ID` milik Admin Platform pada database uji.

```powershell
bru run smoke --env Local --env-var baseUrl=http://localhost:18084 --bail --noproxy --sandbox developer
bru run user-management/04-tenant-accounts-and-roles --env Local --env-var baseUrl=http://localhost:18084 --bail --noproxy --sandbox developer
```

## Blocked By

[Tiket 02](02-onboarding-tenant.md) untuk tenant, akun pemilik, dan konteks login.
