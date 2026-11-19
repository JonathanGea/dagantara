---
name: spring-boot-api
description: Gunakan saat membuat atau mengubah REST controller, endpoint, request/response DTO, validation, pagination, atau kontrak error pada backend Spring Boot Dagantara.
---

# Spring Boot API

## Tujuan

Membuat REST API yang konsisten, aman, dan sesuai spesifikasi modul Dagantara. Skill ini **bukan** izin untuk membuat endpoint yang belum diputuskan.

## Sebelum coding

1. Baca `AGENTS.md`, `backend/AGENTS.md`, `docs/README.md`, dan spesifikasi modul terkait.
2. Catat Functional Specification ID dan Acceptance Scenario ID yang hendak dipenuhi.
3. Periksa routing, response format, DTO, exception handler, validation, dan coding style aktual; ikuti yang sudah ada.
4. Jika kontrak endpoint belum ada, jelaskan keputusan yang perlu dibuat. Jangan menetapkan URL, payload, atau fitur baru sebagai keputusan domain.

## Aturan implementasi

- Controller tipis: menerima request, validasi DTO, delegasi ke service, lalu bentuk HTTP response.
- Jangan expose JPA entity secara langsung ke API. Gunakan request/response DTO dan mapping yang jelas.
- Gunakan Bean Validation untuk bentuk data; validasi business rules dan tenant boundary di service.
- Gunakan semantik HTTP yang konsisten dengan kode eksisting: 400 untuk input tidak valid, 401 bila belum terautentikasi, 403 bila tidak berizin, 404 bila resource tidak dapat ditemukan/diakses sesuai kebijakan, 409 untuk konflik state/integritas yang memang dipetakan sebagai konflik.
- Buat error handling terpusat dan response yang stabil tanpa stack trace atau informasi internal ke client.
- Untuk list endpoint yang diminta, saring berdasarkan tenant di sumber data; gunakan pagination, batas ukuran, serta sort field yang di-allowlist.
- Jangan percaya `tenant_id`, `role_id`, atau permission dari body/header client sebagai bukti hak akses. Role target harus tetap divalidasi terhadap konteks dan scope.
- Periksa update parsial, idempotensi, serta transaksi saat use case memang membutuhkannya. Jangan menambah proses asynchronous secara spekulatif.
- Endpoint hanya mengembalikan data yang memang diizinkan; cegah mass assignment dengan DTO yang spesifik.

## Domain-sensitive cases

- User creation (UM-01/UM-02): cek `users.create`, konteks tenant, dan validitas role target sebelum persist.
- Product (PM-01): request dan query harus menjaga Independent Catalog.
- Primary Category (PM-02): jangan implementasikan detail storage yang masih menjadi dependency.

## Definition of done

- Kontrak request/response selaras dengan spesifikasi yang benar-benar final.
- Ada pengujian untuk happy path, validation error, unauthorized/forbidden, dan lintas tenant (bila relevan).
- Tidak ada endpoint atau field tambahan yang dibuat semata karena terlihat pada Admin UI Structure.
- Laporkan path, ID spesifikasi, status test, dan unresolved dependency.
