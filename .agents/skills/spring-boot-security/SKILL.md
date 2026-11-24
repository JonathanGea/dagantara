---
name: spring-boot-security
description: Gunakan untuk autentikasi Spring Security/JWT, authorization role-permission, validasi tenant, password handling, dan perlindungan endpoint backend Dagantara.
---

# Spring Boot Security

## Tujuan

Menegakkan authentication, authorization, dan tenant isolation pada semua jalur akses data, sesuai `docs/specs/user-management.md` dan kontrak modul terkait.

## Sebelum implementasi

1. Baca `AGENTS.md`, `backend/AGENTS.md`, dan spesifikasi terkait.
2. Inspeksi konfigurasi keamanan aktual (JWT, session, filter chain, auth principal, exception handling). **Jangan mengasumsikan JWT sudah terpasang** ketika backend masih skeleton.
3. Petakan akses tiap use case ke permission yang memang dinyatakan di spesifikasi. Jangan mengarang permission baru tanpa requirement.
4. Identifikasi sumber tenant context dan aturan validasinya; jangan menerima tenant claim/header yang belum diverifikasi.

## Authorization model

- Tempatkan pemeriksaan izin bisnis di `modules/<nama_module>/service`. Gunakan `common/security` untuk konfigurasi dan komponen keamanan lintas modul.
- Setiap user memiliki satu `users.role_id` aktif.
- Permission efektif berasal dari `users.role_id -> roles -> role_permissions -> permissions`.
- Untuk pembuatan user, butuh `users.create` (UM-01), lalu validasi role dan tenant target (UM-02/UM-06).
- Role scope/tenant harus kompatibel dengan user target; periksa di service, bukan hanya di anotasi controller.
- User platform `tenant_id = NULL` dan role sistem `tenant_id = NULL` tidak otomatis memberikan akses tak terbatas.
- Prinsip deny by default: operasi tanpa aturan akses yang pasti tidak boleh dibuka hanya untuk mempermudah development.
- Frontend menyembunyikan menu bukan mekanisme otorisasi backend.

## Implementasi aman

- Simpan password memakai `PasswordEncoder` yang aman; jangan simpan plaintext atau menuliskannya ke log.
- Jika JWT dipakai, verifikasi signature, expiry, issuer/audience sesuai konfigurasi eksisting, dan jangan percaya claim yang tidak tervalidasi.
- Validasi tenant pada lookup dan mutasi entity, termasuk hubungan entity, listing, dan resource ID yang bisa ditebak.
- Utamakan hasil 401 untuk unauthenticated dan 403 untuk authenticated tetapi tidak memiliki izin; cegah kebocoran detail resource tenant lain.
- Gunakan least privilege pada konfigurasi route dan service; cegah privilege escalation saat create/update role atau assign role.
- Jangan commit key/secret; gunakan konfigurasi lingkungan dan jangan memaparkan token dalam response/log.

## Test wajib saat fitur tersedia

- Unauthenticated request ditolak.
- Authenticated tanpa `users.create` tidak dapat membuat user (UM-AT-02).
- Role yang salah scope/tenant ditolak (UM-AT-05).
- Tenant A tidak dapat mengakses atau mengubah data tenant B (UM-AT-04).
- Jalur sukses berizin diverifikasi (UM-AT-01/UM-AT-03).

## Definition of done

Laporkan aturan permission yang ditegakkan, tenant boundary, hasil security tests, dan keputusan autentikasi/otorisasi yang belum dinyatakan dalam spesifikasi.
