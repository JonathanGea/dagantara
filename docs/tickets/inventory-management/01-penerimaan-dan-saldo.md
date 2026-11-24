# Inventory 01 — Penerimaan beberapa SKU dan saldo

Spec induk: [Inventory Management](../../specs/inventory-management.md)

Status: done

## What to Build

Implementasikan IM-01, IM-02, riwayat dan saldo per SKU dari IM-04, serta kontrak baca IM-05. Buat gudang bawaan saat penerimaan pertama berhasil. Penerimaan menulis riwayat dan saldo secara atomik serta mempertahankan penguncian SKU. Inventory 02 melengkapi koreksi dan agregat.

## Acceptance Criteria

- [x] IM-AT-01 — Terima beberapa SKU, termasuk varian produk Draft dan tanpa opsi, dalam satu penerimaan dan gudang tenant. Gabungkan SKU berulang berdasarkan UUID varian. Baca saldo awal nol, saldo baru per SKU, dan riwayat penerimaan melalui API.
- [x] IM-AT-02 — Tolak keseluruhan penerimaan dengan `400` jika satu SKU di luar tenant atau jumlah tidak valid. Baca ulang riwayat dan saldo untuk membuktikan penolakan atomik.
- [x] IM-AT-03 — Ulangi key dan payload yang sama, lalu verifikasi ID penerimaan dan saldo yang hanya bertambah sekali. Payload berbeda dengan key sama menghasilkan `409`. Key dapat dipakai kembali setelah validasi gagal.
- [x] Bagian baca IM-AT-07 — Riwayat tenant B mengecualikan penerimaan A dan detail penerimaan A memberi `404`. Request tanpa login memberi `401` dan akun tanpa izin memberi `403`.
- [x] PM-AT-06 — Terima stok berdasarkan SKU yang telah diperbaiki. Perubahan SKU berikutnya ditolak `409` dan stok tetap terhubung ke UUID yang sama. Buktikan serialisasi yang mencegah perubahan SKU berlomba dengan penerimaan.
- [x] IM-05 dan bagian penerimaan IM-AT-09 — Kontrak baca membatasi hasil ke varian milik tenant, memetakan saldo nol dan positif, serta melihat transaksi setelah commit. Penerimaan paralel dan retry pada PostgreSQL mempertahankan setiap penambahan tanpa catatan ganda.
- [x] Jalankan skenario Bruno yang relevan dan uji transaksi PostgreSQL.

## Kontrak untuk Inventory 02 dan Product 03

Kelas Inventory berada di package `com.dagantara.backend.modules.inventory`.

- Product 03 dapat menginjeksi `service.InventoryAvailabilityService` dan memakai `availability(UUID tenantId, Collection<UUID> variantIds)`. Kirim tenant dari pemetaan URL di server. Hasil `Map<UUID, Availability>` memetakan varian milik tenant ke `IN_STOCK` atau `OUT_OF_STOCK` dalam satu pembacaan PostgreSQL. ID asing dan ID tak dikenal dilewati. Varian yang belum bersaldo termasuk dalam hasil dengan `OUT_OF_STOCK`.
- Inventory 02 dapat memakai `repository.InventoryReceiptRepository.findReceipt(tenantId, receiptId)` dan `listLines(tenantId, receiptId)`. `inventory_receipts` menyimpan `id`, `tenant_id`, `warehouse_id`, `received_by`, dan `created_at`. Baris awal bersifat tetap di `inventory_receipt_lines` dengan `id`, `tenant_id`, `receipt_id`, `variant_id`, `sku`, dan `quantity`. FK koreksi dapat mengacu pada `inventory_receipts(tenant_id, id)` serta `inventory_receipt_lines(tenant_id, receipt_id, id)`.
- `repository.InventoryStockRepository.quantities(tenantId, variantIds)` mengembalikan kuantitas per UUID varian, termasuk nol untuk varian tanpa saldo. Saldo persisten memakai `inventory_stock_balances(tenant_id, warehouse_id, variant_id, quantity)` dengan kunci gudang dan varian. Penerimaan mengunci varian menurut urutan UUID PostgreSQL lewat `CatalogVariantRepository.lockBySkus` sebelum menulis saldo, dan menandai `product_variants.first_stock_recorded_at` lewat `markStockRecorded`. Gunakan urutan kunci varian yang sama untuk perubahan stok lain.
- `service.InventoryService.getReceipt` menyusun `ReceiptDetail.lines` dengan `originalQuantity` dan `effectiveQuantity`, serta `corrections` sebagai larik riwayat. `InventoryService.getProductStock` menyusun `ProductStock.variants` dengan ID varian, SKU, nilai opsi, dan jumlah. `StockOptionValue` memuat `optionId`, `optionName`, `valueId`, dan `label`. Inventory 02 menambahkan riwayat koreksi, jumlah efektif hasil koreksi, dan agregat stok.
- `POST /api/v1/inventory/receipts` merespons `201` dengan baris transaksi awal termasuk saat retry. `Idempotency-Key` wajib berisi 1–255 karakter dan tidak boleh hanya spasi. Identitas payload memperhitungkan urutan baris, teks SKU yang dikirim, dan jumlah per baris. Normalisasi SKU untuk lookup tetap mengikuti katalog. `inventory_receipt_keys` menyimpan `tenant_id`, `account_id`, `idempotency_key`, `request_hash`, dan `receipt_id` khusus untuk operasi penerimaan. FK yang diperiksa saat commit mengikat key ke penerimaan yang sama.
- Migrasi `08-inventory-receipts.sql` diterapkan setelah 06. `TenantPermission` mendaftarkan izin baca, terima, dan koreksi. Provisioning memakai katalog permission untuk owner baru dan migrasi memberi izin tersebut ke role `TENANT_ADMIN` tenant yang sudah ada.

## Verifikasi Inventory 01

PostgreSQL 16 fixture memakai port 55433 dengan migrasi 01–06 dan 08. `InventoryPostgresTest` membuat dan membersihkan schema uji sendiri. Dari `backend/`, jalankan Maven dengan JDK 25 pada `JAVA_HOME` dan setel `INVENTORY01_TEST_DB_URL`, `INVENTORY01_TEST_DB_USERNAME`, serta `INVENTORY01_TEST_DB_PASSWORD` ke database fixture. URL fixture mengaktifkan tes PostgreSQL. Setel juga `PRODUCT01_TEST_DB_URL`, `PRODUCT01_TEST_DB_USERNAME`, dan `PRODUCT01_TEST_DB_PASSWORD` untuk menjalankan regresi Product 01 pada schema terpisah.

```powershell
.\mvnw.cmd -B clean package
```

Jalankan Bruno dari `bruno/` dengan kredensial operator fixture di `INVENTORY01_PLATFORM_USERNAME` dan `INVENTORY01_PLATFORM_PASSWORD`. Regresi Product 01 membaca `PRODUCT01_PLATFORM_USERNAME` dan `PRODUCT01_PLATFORM_PASSWORD`. Gunakan backend fixture terisolasi. Skenario membuat tenant dan produk unik tiap run, sehingga database fixture dapat dibuang setelah verifikasi.

```powershell
bru run smoke --env Local --env-var baseUrl=http://localhost:18093 --bail --noproxy --sandbox developer
bru run inventory-management/01-penerimaan-dan-saldo --env Local --env-var baseUrl=http://localhost:18093 --bail --noproxy --sandbox developer
bru run product-management/01-katalog-draft-dan-sku --env Local --env-var baseUrl=http://localhost:18093 --bail --noproxy --sandbox developer
```

| Skenario | Bukti | Hasil |
| --- | --- | --- |
| IM-AT-01 | Bruno membaca saldo awal, penerimaan 8 Hitam/M, 5 Putih/L, dan 3 varian bawaan, lalu riwayat dan saldo. PostgreSQL memeriksa satu gudang dan penggabungan SKU berdasarkan varian. | Lulus |
| IM-AT-02 dan IM-AT-03 | Bruno membuktikan rollback SKU lintas tenant dan jumlah pecahan, key dapat dipakai lagi, retry mengembalikan transaksi awal, serta konflik payload memberi `409`. PostgreSQL memeriksa rollback saat saldo melebihi batas, dan cakupan key per akun serta tenant. | Lulus |
| IM-AT-07 bagian baca | Riwayat B berisi penerimaan B, detail dan saldo A dari B memberi `404`, tanpa login memberi `401`, dan kurang izin atau tenant memberi `403`. | Lulus |
| PM-AT-06 | Bruno memeriksa SKU baru dan `409 SKU_LOCKED` sesudah penerimaan. PostgreSQL membuktikan serialisasi pada kedua urutan transaksi penerimaan dan perubahan SKU. | Lulus |
| IM-05 dan IM-AT-09 bagian penerimaan | PostgreSQL membuktikan status nol dan positif, visibilitas setelah commit, pemfilteran tenant, retry paralel, penerimaan paralel SKU yang sama, dan pembuatan satu gudang oleh dua penerimaan awal paralel. | Lulus |

Maven lulus 16 tes PostgreSQL, terdiri dari 10 tes Inventory 01 dan 6 regresi Product 01. Bruno CLI 4.3.0 lulus 1 request smoke, 48 request Inventory 01, dan 49 request Product 01.

## Blocked By

- [Product 01](../product-management/01-katalog-draft-dan-sku.md) untuk UUID varian, lookup SKU, dan mekanisme koordinasi transaksi yang menyentuh varian.
- [User Management](../../specs/user-management.md#implementation-tickets) untuk login, identitas tenant, dan permission role aktif.
