# 03 — Akun, role, dan delegasi operator platform

[Spesifikasi User Management](../../specs/user-management.md)

Status: done

## What to Build

Admin Platform mengelola akun dan role operator. Pisahkan izin provisioning dari izin pemulihan Help Center. Tegakkan kewenangan khusus akun Admin Platform pertama dan siklus akun operator. Cakupan meliputi UM-01 hingga UM-05, UM-10, dan bagian platform dari UM-11.

## Acceptance Criteria

- [x] Akun operator dibuat dalam cakupan platform dengan satu role sah dan password sementara. Pembuatan ditolak tanpa izin atau dengan role di luar cakupan (UM-AT-01, UM-AT-02, UM-AT-05).
- [x] Pembuatan role, perubahan permission, dan penetapan role diperiksa terpisah. Pengelola tidak dapat memberi atau mengelola izin di luar kewenangannya. Perubahan efektif pada aksi berikutnya dan izin provisioning berbeda dari izin reset admin tenant (UM-AT-03, UM-AT-08).
- [x] Pengelola berizin dapat mendaftar, melihat, mereset, menonaktifkan, mengaktifkan kembali, dan mengarsipkan operator. Reset, penonaktifan, dan pengarsipan mencabut akses sesi lama. Akun arsip tetap menyimpan riwayat dan ditolak saat dicoba diaktifkan kembali (UM-AT-11).
- [x] Hanya akun Admin Platform pertama yang dapat memberi atau mencabut role `SUPERADMIN`. Akun tersebut terlindung dari perubahan role, penonaktifan, pengarsipan, dan reset melalui pengelolaan operator (UM-AT-06, UM-AT-12).

## Blocked By

[Tiket 01](01-bootstrap-platform-admin.md) untuk Admin Platform pertama.
