---
name: spring-boot-testing
description: Gunakan saat membuat atau menjalankan unit, MVC, integration, repository, security, dan acceptance tests untuk backend Spring Boot Dagantara, termasuk pengujian API HTTP dengan Bruno.
---

# Spring Boot Testing

## Prinsip

Pengujian harus membuktikan business rules dan acceptance scenarios yang telah diputuskan, bukan mengunci asumsi fitur yang belum disepakati.

## Persiapan

1. Baca `AGENTS.md`, `backend/AGENTS.md`, `docs/README.md`, dan modul relevan.
2. Catat ID Acceptance Scenario (mis. UM-AT-01, PM-AT-04) pada nama test atau deskripsi test agar traceable.
3. Inspeksi framework, build tool, test profile, fixture, dan infrastruktur yang tersedia. Jangan menambahkan dependency besar tanpa alasan tugas.
4. Pilih level uji paling rendah yang memberi bukti memadai; tambahkan integration test untuk perilaku persistence/security yang tidak dapat dibuktikan oleh mock.

## Strategi per lapisan

- **Unit/service:** business rules, validation, role assignment, domain edge cases.
- **Web/MVC:** serialization, request validation, HTTP status, error contract, dan endpoint authorization.
- **API HTTP:** gunakan Bruno untuk memverifikasi kontrak dan acceptance scenario melalui request ke backend yang berjalan.
- **Repository/integration:** FK/UNIQUE, transaction, query/filter tenant, relasi JPA, dan migration.
- **Security integration:** authenticated vs unauthenticated, permission denied, role scope, tenant cross-access.
- **Database:** gunakan PostgreSQL yang terisolasi (mis. Testcontainers) bila tersedia dan relevan; jangan menyamakan perilaku in-memory DB dengan PostgreSQL production.

## Skenario API HTTP di Bruno

- Simpan smoke test di `bruno/smoke/` dan acceptance scenario di `bruno/<modul>/<ID>-<nama-skenario>/`. Satu folder skenario memuat semua request yang diperlukan agar bisa dijalankan sendiri.
- Untuk skenario dengan beberapa request, urutkan persiapan, aksi, dan verifikasi dengan awalan angka pada nama file dan nilai `seq` yang sesuai. Siapkan autentikasi dan data uji di dalam skenario bila diperlukan. Periksa respons persiapan sebelum meneruskan nilai yang dibutuhkan, seperti ID atau token, melalui variabel runtime Bruno (`bru.setVar`).
- Jalankan skenario yang mengubah data di lingkungan uji terisolasi. Gunakan fixture unik tiap run dan siapkan cara membersihkan fixture atau mereset lingkungan bila run terhenti. Simpan kredensial uji di secret lokal Bruno atau variabel proses.
- Dari `bruno/`, jalankan `bru run <folder-skenario> --env Local`, atau pilih environment `Local` dan jalankan folder itu lewat Collection Runner. Untuk smoke test, gunakan `bru run smoke --env Local`. Aktifkan `--bail` jika skenario harus berhenti saat setup gagal dan pembersihan dapat dilakukan di luar run.

## Skenario minimum berdasarkan dokumentasi

| ID | Hal yang harus dibuktikan |
| --- | --- |
| UM-AT-01 | Pengguna berizin dapat membuat user dengan tenant dan role valid. |
| UM-AT-02 | Tanpa `users.create` operasi ditolak tanpa perubahan data. |
| UM-AT-03 | Permission efektif mengikuti role dan `role_permissions`. |
| UM-AT-04 | Tenant A tidak dapat membaca/mengubah user tenant B. |
| UM-AT-05 | Assignment role salah scope/tenant ditolak. |
| PM-AT-01 | Produk dimiliki tenant yang benar dan tidak dibagikan lintas tenant. |
| PM-AT-02 | Multi-category dengan tepat satu primary; implementasi test schema menunggu keputusan storage PM-02. |
| PM-AT-03 | Satu media bisa terkait beberapa option value. |
| PM-AT-04 | Satu option value bisa terkait beberapa media tanpa memindahkan ownership media. |

## Praktik test

- Gunakan data deterministik, fixture terisolasi, dan cleanup. Pastikan setiap skenario dapat dijalankan tanpa bergantung pada urutan antarskenario.
- Sertakan negative cases: invalid input, duplicate relation, missing FK, unauthorized, forbidden, dan cross-tenant association.
- Hindari mengganti semua dependency dengan mock pada test yang mengklaim menguji constraint database.
- Jalankan tes Spring Boot lewat command build yang dipakai repository, misalnya `./mvnw test`.
- Bila environment test, migration, atau server belum tersedia, tandai **not run** beserta sebabnya; jangan membuat laporan hijau palsu.

## Definition of done

Laporkan test yang ditambah, scenario ID yang dicakup, perintah aktual, hasil test, dan gap pengujian/keputusan yang masih terbuka.
