# 05 — Pemulihan admin tenant melalui Help Center

[Spesifikasi User Management](../../user-management.md)

Status: ready-for-agent

## What to Build

Operator Help Center memulihkan password Pemilik Tenant atau Admin Tenant tambahan setelah memeriksa pemohon. Batasi akses operator pada informasi yang diperlukan untuk pemulihan dan simpan audit tindakan. Cakupan meliputi UM-09 dan bagian kredensial dari UM-02 dan UM-05.

## Acceptance Criteria

- [ ] Operator dengan izin pemulihan dapat memeriksa informasi yang diperlukan dan mencatat bahwa identitas admin telah diperiksa. Operator dengan izin provisioning saja dan permintaan tanpa verifikasi ditolak tanpa perubahan akun (UM-AT-08, UM-AT-10).
- [ ] Reset Pemilik Tenant maupun Admin Tenant tambahan mencabut password dan sesi lama. Password sementara hanya membuka alur ganti password (UM-AT-10).
- [ ] Audit mencatat operator, akun target, dan waktu reset. Kredensial tetap di luar audit dan log. Informasi pemulihan tersedia melalui Help Center (UM-AT-10).

## Blocked By

- [Tiket 02](02-onboarding-tenant.md) untuk akun pemilik.
- [Tiket 03](03-operator-platform.md) untuk role operator Help Center.
- [Tiket 04](04-akun-dan-role-tenant.md) untuk Admin Tenant tambahan.
