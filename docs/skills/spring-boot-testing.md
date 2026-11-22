---
name: spring-boot-testing
description: Buat atau jalankan unit, MVC, integration, repository, dan security test untuk backend Spring Boot Dagantara.
---

# Spring Boot Testing

## Prinsip

Pengujian harus membuktikan business rules dan acceptance scenarios yang telah diputuskan, bukan mengunci asumsi fitur yang belum disepakati.

## Persiapan

1. Baca `AGENTS.md`, `backend/AGENTS.md`, `docs/README.md`, dan modul relevan.
2. Rujuk kriteria penerimaan atau ID skenario yang dibuktikan pada nama test atau deskripsi test.
3. Inspeksi framework, build tool, test profile, fixture, dan infrastruktur yang tersedia. Jangan menambahkan dependency besar tanpa alasan tugas.
4. Pilih level uji paling rendah yang memberi bukti memadai; tambahkan integration test untuk perilaku persistence/security yang tidak dapat dibuktikan oleh mock.

## Strategi per lapisan

- **Unit/service:** business rules, validation, role assignment, domain edge cases.
- **Web/MVC:** serialization, request validation, HTTP status, error contract, dan endpoint authorization.
- **API HTTP:** ikuti `bruno-api-testing` untuk memverifikasi perilaku melalui backend yang berjalan. Pakai integration test untuk melengkapi bukti efek data yang tidak bisa diamati lewat API.
- **Repository/integration:** FK/UNIQUE, transaction, query/filter tenant, relasi JPA, dan migration.
- **Security integration:** authenticated vs unauthenticated, permission denied, role scope, tenant cross-access.
- **Database:** gunakan PostgreSQL yang terisolasi (mis. Testcontainers) bila tersedia dan relevan; jangan menyamakan perilaku in-memory DB dengan PostgreSQL production.

## Praktik test

- Gunakan data deterministik, fixture terisolasi, dan cleanup. Pastikan setiap skenario dapat dijalankan tanpa bergantung pada urutan antarskenario.
- Pilih kasus penolakan yang relevan dengan spec, seperti input invalid, relasi duplikat, pelanggaran constraint, penolakan akses, atau asosiasi lintas tenant.
- Hindari mengganti semua dependency dengan mock pada test yang mengklaim menguji constraint database.
- Jalankan tes Spring Boot lewat command build yang dipakai repository, misalnya `./mvnw test`.
- Bila environment test, migration, atau server belum tersedia, tandai **not run** beserta sebabnya; jangan membuat laporan hijau palsu.

## Definition of done

Laporkan test yang ditambah, scenario ID yang dicakup, perintah aktual, hasil test, dan gap pengujian/keputusan yang masih terbuka.
