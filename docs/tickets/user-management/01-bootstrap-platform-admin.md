# 01 — Setup dan login Admin Platform pertama

[Spesifikasi User Management](../../user-management.md)

Status: done

## What to Build

Siapkan satu akun Admin Platform pertama dengan role bawaan `SUPERADMIN` dalam cakupan platform. Setup dapat dijalankan ulang dengan aman. Akun masuk melalui `POST /api/v1/auth/login` dengan username dan password. Tiket ini meliputi setup dan login dari UM-07 serta bagian setup dan login dari UM-AT-06. Pengelolaan akun platform dan penetapan role Admin Platform dilanjutkan pada tiket 03.

## Acceptance Criteria

- [x] Setup pada database bersih menyimpan satu akun awal dengan password ter-hash, role bawaan `SUPERADMIN`, dan penanda akun awal yang eksplisit. Akun dan role berada dalam cakupan platform.
- [x] Setup ulang mempertahankan ID, username, password, dan role akun awal. Percobaan setup bersamaan tetap menghasilkan satu akun awal.
- [x] Username dan password akun tersimpan menghasilkan respons login sukses. Login tetap berhasil setelah konfigurasi setup dilepas.
- [x] Password salah dan username tak dikenal memberikan `401`. Input tanpa username atau password memberikan `400`.

## Blocked By

Bisa langsung dimulai.
