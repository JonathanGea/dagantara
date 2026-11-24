---
name: spring-boot-database
description: Gunakan untuk desain dan perubahan PostgreSQL, JPA/Hibernate entity, relationship, repository query, constraint, transaction, atau migration backend Dagantara.
---

# Spring Boot Database

## Source of truth

- `docs/user-management.md`: ERD dan aturan UM-01 s.d. UM-06.
- `docs/product-management.md`: model data, Primary Category, relasi media, dan identitas varian.
- `docs/inventory-management.md`: gudang, penerimaan, koreksi, dan saldo per varian.
- Detail yang tidak dicantumkan sebagai keputusan final **jangan** diisi dengan asumsi tanpa persetujuan.

## Workflow

1. Baca AGENTS root/backend dan spesifikasi yang menyebut tabel serta relasi terkait.
2. Inspeksi schema, entity, naming, migration tool (jika ada), dan query yang saat ini dipakai.
3. Buat rencana minimal: tabel/kolom/FK/index/constraint yang dibutuhkan **hanya** dari keputusan final.
4. Periksa dampak perubahan pada data existing, migration/rollback, tenant isolation, dan referential integrity.
5. Implementasikan migration melalui tool yang sudah dipakai project (Flyway/Liquibase jika tersedia). Jangan bergantung pada `ddl-auto=update` untuk perubahan production.
6. Tambahkan integration test yang mengecek constraint dan isolasi tenant bila lingkungan test tersedia.

## Model yang sudah disepakati

- User management: `tenants`, `tenant_domains`, `users`, `roles`, `permissions`, `role_permissions`.
- `users.role_id` menunjuk satu role aktif; `role_permissions` unik untuk pasangan `(role_id, permission_id)`.
- `users.tenant_id` dapat NULL untuk akun platform; `roles.tenant_id` dapat NULL untuk role sistem. Penanganan NULL tidak sama dengan izin akses global.
- Product management: `products` dimiliki tenant; `product_categories` menghubungkan produk dan kategori secara many-to-many.
- Simpan satu Primary Category pada `products.primary_category_id` dengan FK gabungan ke `product_categories` yang diperiksa saat commit sesuai PM-02.
- `product_media` dan `product_option_values` terhubung many-to-many melalui `media_option_values`, dengan media tetap milik produk.
- Jangan menyimpulkan `tenant_id` wajib ada pada tabel yang belum dirinci di ERD; identifikasi kebutuhan isolasi dan minta keputusan bila skema belum final.

## Aturan query, transaction, dan constraint

- Gunakan subpackage `entity` dan `repository` di `modules/<nama_module>` untuk model persistence dan akses data modul.
- Terapkan filter tenant pada query repository yang mengakses data tenant; validasi kedua sisi saat mengaitkan FK agar tidak dapat membuat relasi lintas tenant tanpa izin.
- Jangan mengandalkan filter HTTP saja untuk tenant isolation.
- Pertahankan FK, UNIQUE, dan NOT NULL yang sudah disepakati; beri indeks berdasar query yang nyata, bukan secara spekulatif.
- Gunakan @Transactional di service untuk operasi atomik; tentukan locking/concurrency control dari kebutuhan terukur.
- Cegah N+1 dan pagination tidak stabil; verifikasi query/filter dengan integration test.
- Migration harus bisa dijalankan konsisten pada database bersih; perubahan data existing memerlukan strategi migrasi yang jelas.

## Definition of done

Sertakan daftar perubahan schema, alasan dari ID requirement, pengujian query/constraint/tenant, serta keputusan yang masih belum final. Jangan klaim migration berhasil tanpa eksekusi.
