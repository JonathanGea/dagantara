# Product 01 — Katalog Draft, kategori, opsi, dan SKU

Spec induk: [Product Management](../../product-management.md)

Status: done

## What to Build

Implementasikan API admin kategori, produk Draft, detail, opsi, dan varian untuk PM-01, PM-02, PM-05, dan PM-06. Daftarkan permission katalog sesuai User Management. Sediakan kontrak varian untuk Inventory 01, termasuk lookup SKU terisolasi tenant dan koordinasi kunci varian untuk serialisasi perubahan SKU dan pencatatan stok.

## Acceptance Criteria

- [x] Admin tenant dengan `catalog.manage` bisa membuat kategori dan produk Draft serta memperbarui detail, kategori, dan nilai opsi sesuai kontrak. Akun dengan `catalog.read` bisa membaca daftar dan detail. Pagination hanya mencakup tenant pelaku.
- [x] PM-AT-02 — Buat dan ganti kategori beserta Primary Category dalam satu transaksi. Primary Category wajib menjadi anggota daftar kategori. Referensi kategori tenant lain atau primary di luar daftar menghasilkan `400` dan kategori sebelumnya tetap tersimpan. Buktikan constraint PostgreSQL.
- [x] PM-AT-05 — Simpan varian AUTO dan MANUAL, dengan UUID stabil, pola dan normalisasi SKU yang ditentukan, serta varian bawaan untuk produk tanpa opsi. Duplikasi SKU dalam tenant atau kombinasi memberi `409`. SKU yang sama bisa digunakan oleh tenant lain. Pembaruan label mempertahankan ID nilai dan SKU tersimpan.
- [x] Bagian katalog PM-AT-01 dan PM-AT-09 — Daftar B hanya menampilkan katalog B, detail produk A dari akun B memberi `404`, perubahan oleh akun baca-saja memberi `403`, dan request admin tanpa login memberi `401`. Baca ulang data setelah penolakan.
- [x] Bagian awal PM-AT-06 — Perbaiki SKU sebelum penerimaan pertama tanpa mengganti ID varian. Inventarisasi dan uji kontrak kunci bersama. Inventory 01 memverifikasi penolakan perubahan SKU setelah penerimaan.
- [x] Jalankan skenario Bruno yang relevan dan pengujian constraint PostgreSQL.

## Kontrak Inventory dan Storefront

Semua kelas katalog berada di package `com.dagantara.backend.modules.catalog`.

- Gunakan `repository.CatalogVariantRepository.lockBySkus(UUID tenantId, Collection<String> skus)` di dalam transaksi penerimaan PostgreSQL dengan isolasi `READ COMMITTED`. Parameter dapat berisi SKU mentah. Repository memangkas spasi, menormalkan ke kapital ASCII, dan mengunci semua varian yang ditemukan sekaligus dengan `FOR UPDATE` menurut urutan UUID. `service.SkuCodes.normalizeSku(String)` tersedia untuk kanonisasi payload atau mencocokkan hasil lookup. SKU tersimpan memakai maksimal 255 karakter.
- Cocokkan hasil `VariantRow.sku()` dengan setiap SKU ternormalisasi, tolak seluruh penerimaan jika ada SKU yang hilang, dan gabungkan kuantitas berdasarkan `VariantRow.id()`. Simpan UUID dan salin `VariantRow.sku()` sebagai SKU saat transaksi. FK gabungan Inventory dapat mereferensikan `product_variants(tenant_id, id)`.
- Panggil `CatalogVariantRepository.markStockRecorded(UUID tenantId, Collection<UUID> lockedVariantIds)` pada transaksi yang sama dengan riwayat dan saldo. Kolom `product_variants.first_stock_recorded_at` diisi sekali dan dipertahankan saat saldo menjadi nol. Rollback penerimaan juga membatalkan pengisian penanda. Koreksi SKU memegang kunci baris yang sama melalui `lockForProduct(tenantId, productId, variantId)` dan mengembalikan `409 SKU_LOCKED` setelah penanda terisi.
- Gunakan `listForProduct(tenantId, productId)` untuk daftar varian, `findByIds(tenantId, variantIds)` untuk pemeriksaan sekumpulan ID, dan `findBySku(tenantId, sku)` untuk lookup baca. `VariantRow` mengembalikan ID produk, ID varian, SKU, mode, serta ID nilai opsi menurut urutan opsi. `CatalogRepository.findProduct(tenantId, productId)` membedakan produk tanpa kombinasi dari produk di luar tenant, dan `listOptions(tenantId, productId)` memberi dimensi, label, dan kode nilai. `product_variant_option_values` juga tersedia untuk agregasi relasional per nilai.
- Product 02 dapat memakai `CatalogAccess.require(actor, permission)` dan `CatalogRepository.findSelectedValues(tenantId, productId, ids)`. FK media dapat mereferensikan `product_option_values(product_id, id)`. Product 03 dapat memakai `CatalogRepository.lockProduct(tenantId, productId)` dalam transaksi publikasi untuk menserialisasi perubahan status dengan pembaruan detail.

## Verifikasi Product 01

Migrasi 01–06 diterapkan pada PostgreSQL 16 fixture. `CatalogPostgresTest` membuat dan membersihkan schema tersendiri pada database yang ditentukan lewat `PRODUCT01_TEST_DB_URL`, `PRODUCT01_TEST_DB_USERNAME`, dan `PRODUCT01_TEST_DB_PASSWORD`. Gunakan database fixture terisolasi. Tes integrasi dilewati jika URL fixture tidak disetel.

Jalankan Maven dari `backend/` dengan JDK 25 pada `JAVA_HOME`. Jalankan Bruno dari `bruno/` dengan kredensial operator fixture pada `PRODUCT01_PLATFORM_USERNAME` dan `PRODUCT01_PLATFORM_PASSWORD`. Ganti port pada override `baseUrl` mengikuti backend fixture.

```powershell
.\mvnw.cmd -B clean package
bru run smoke --env Local --env-var baseUrl=http://localhost:18091 --bail --noproxy --sandbox developer
bru run product-management/01-katalog-draft-dan-sku --env Local --env-var baseUrl=http://localhost:18091 --bail --noproxy --sandbox developer
```

| Skenario | Bukti | Hasil |
| --- | --- | --- |
| PM-AT-01 bagian katalog dan PM-AT-09 | Isolasi daftar serta detail, token pada URL yang salah, permission efektif, dan pembacaan setelah penolakan | Pass |
| PM-AT-02 | Primary Category atomik dan tenant-scoped via Bruno serta FK gabungan dan FK deferred pada PostgreSQL | Pass |
| PM-AT-05 | Varian AUTO, MANUAL, dan bawaan, konflik SKU dan kombinasi, label dengan ID stabil, plus constraint keunikan PostgreSQL | Pass |
| PM-AT-06 bagian Product 01 | Koreksi SKU dengan UUID tetap, kunci dua transaksi, rollback penanda dan pengisian sekali pada PostgreSQL | Pass |
| PM-07 bagian perubahan detail | `409 PRODUCT_NOT_READY` saat harga produk terbit dihapus melalui HTTP, diikuti pembacaan ulang harga dan detail lama | Pass |
| PM-AT-06 penerimaan aktual | Dibuktikan lewat alur stok Inventory 01 | Not run |

Maven menjalankan enam tes PostgreSQL. Bruno CLI 4.3.0 menjalankan Product 01 dengan 49 request lulus serta regresi `user-management/04-tenant-accounts-and-roles` dengan 119 request lulus.

## Blocked By

- [User Management](../../user-management.md#implementation-tickets) untuk login, identitas tenant, dan permission role aktif.
