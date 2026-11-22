# 02 — Pengajuan, provisioning, dan login tenant

[Spesifikasi User Management](../../user-management.md)

Status: done

## What to Build

Bangun alur pengajuan publik, peninjauan operator, dan onboarding langsung dengan provisioning atomik. Pemilik login melalui URL tenant dan mengganti password sementara. Cakupan meliputi UM-02, UM-06, UM-08, dan pembuatan Pemilik Tenant pada UM-11.

## Acceptance Criteria

- [x] Pengajuan publik menerima data toko, nomor HP, dan usulan domain sebelum ada akun. Operator berizin dapat meninjau, memperbarui usulan, dan menutup pengajuan. Pengajuan yang masih ditinjau atau ditutup belum memberi akses. Pengajuan yang ditutup ditolak saat dicoba diprovisioning (UM-AT-07, UM-AT-13).
- [x] Provisioning dari pengajuan dan onboarding langsung menyimpan tenant, URL login, role `TENANT_ADMIN`, dan satu akun Pemilik Tenant dalam satu transaksi. Pengajuan yang diprovisioning ditandai selesai. Konflik URL, persetujuan ulang, dan kegagalan tidak menyisakan data parsial atau ganda (UM-AT-07).
- [x] Login memakai pemetaan domain toko atau pasangan hostname dan jalur Dagantara. Dua tenant dapat memakai username yang sama. Kredensial dan sesi hanya berlaku di konteks tenant yang sesuai (UM-AT-09).
- [x] Password sementara hanya membuka alur ganti password. Sesudah diganti, password baru berlaku dan password sementara ditolak (UM-AT-01, UM-AT-09).

## Blocked By

[Tiket 01](01-bootstrap-platform-admin.md) untuk autentikasi operator provisioning.
