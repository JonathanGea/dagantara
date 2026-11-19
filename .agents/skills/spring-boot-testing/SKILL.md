---
name: spring-boot-testing
description: Gunakan saat membuat atau menjalankan unit, MVC, integration, repository, security, dan acceptance tests untuk backend Spring Boot Dagantara.
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
- **Repository/integration:** FK/UNIQUE, transaction, query/filter tenant, relasi JPA, dan migration.
- **Security integration:** authenticated vs unauthenticated, permission denied, role scope, tenant cross-access.
- **Database:** gunakan PostgreSQL yang terisolasi (mis. Testcontainers) bila tersedia dan relevan; jangan menyamakan perilaku in-memory DB dengan PostgreSQL production.

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

- Gunakan data deterministik, isolated fixture, dan cleanup; jangan bergantung pada urutan test.
- Sertakan negative cases: invalid input, duplicate relation, missing FK, unauthorized, forbidden, dan cross-tenant association.
- Hindari mengganti semua dependency dengan mock pada test yang mengklaim menguji constraint database.
- Pastikan test dapat dijalankan lewat command build yang dipakai repository (`./mvnw test`, `mvn test`, atau Gradle sesuai yang tersedia).
- Bila environment test, migration, atau server belum tersedia, tandai **not run** beserta sebabnya; jangan membuat laporan hijau palsu.

## Definition of done

Laporkan test yang ditambah, scenario ID yang dicakup, perintah aktual, hasil test, dan gap pengujian/keputusan yang masih terbuka.
