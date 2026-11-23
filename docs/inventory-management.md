# Inventory Management

## Problem Statement

Ketika kiriman datang, petugas toko perlu mencatat kuantitas barang berdasarkan SKU yang tertera pada barang. Pengelola perlu mengetahui jumlah total produk, melihat rinciannya per warna atau ukuran, dan memperbaiki pencatatan yang keliru dengan riwayat yang dapat ditelusuri.

## Solution

Versi pertama memakai satu gudang dalam tiap tenant. Petugas menerima satu kiriman berisi beberapa SKU dalam satu transaksi. Pengelola melihat saldo per SKU dan hasil penjumlahannya. Koreksi menyimpan selisih dan alasan beserta rujukan ke penerimaan yang dikoreksi. Storefront membaca status ketersediaan dari saldo per varian.

## User Stories

1. Sebagai petugas barang masuk, saya bisa mencatat SKU dan kuantitas untuk seluruh isi kiriman agar stok bertambah pada varian yang sesuai.
2. Sebagai pengawas gudang, saya bisa mengoreksi jumlah yang keliru dan menyertakan alasan agar saldo dan riwayat bisa ditelusuri.
3. Sebagai pengelola toko, saya bisa melihat saldo per SKU, total produk, dan jumlah per nilai opsi agar bisa menjawab berapa unit yang tersedia.
4. Sebagai pengunjung storefront, saya bisa melihat status stok kombinasi yang saya pilih agar tahu apakah varian tersebut tersedia.

## Functional Specification

| ID | Perilaku |
| --- | --- |
| **IM-01** | Setiap tenant memiliki satu gudang versi pertama. Saldo dicatat per varian di gudang tenant. |
| **IM-02** | Penerimaan beberapa SKU dalam tenant yang sama menambah saldo secara atomik. |
| **IM-03** | Koreksi jumlah merujuk penerimaan awal, memiliki alasan, dan dicatat sebagai perubahan saldo yang terpisah. |
| **IM-04** | Pengelola melihat riwayat, saldo SKU, total produk, serta total per nilai opsi dari saldo yang sama. |
| **IM-05** | Storefront dapat membaca status ketersediaan per varian dalam tenant yang sedang dibuka. |

## Implementation Decisions

### Gudang, tenant, dan permission

- Setiap tenant memakai satu gudang bawaan. Inventory menyiapkan gudang tersebut saat transaksi stok pertama berhasil, dengan keunikan gudang per tenant untuk versi ini. API mengarahkan transaksi ke gudang bawaan dan tidak meminta pilihan gudang dari petugas.
- Gunakan tenant dari akun terautentikasi dan permission dari role aktif sesuai [User Management](user-management.md). Cakupan pengelolaan mencakup varian, gudang, penerimaan, koreksi, saldo, daftar, dan total milik tenant akun.
- Daftarkan `inventory.read` untuk membaca saldo dan riwayat, `inventory.receive` untuk membuat penerimaan, serta `inventory.correct` untuk membuat koreksi. Aksi publik hanya memakai operasi baca status ketersediaan melalui Product Management.
- [Product Management](product-management.md#opsi-varian-dan-sku) memiliki definisi varian dan aturan kode SKU. Input SKU dicari dalam tenant akun dan disimpan sebagai referensi UUID varian beserta kode SKU pada saat transaksi. Produk Draft dan produk terbit yang memiliki SKU dapat menerima stok. Varian yang belum memiliki catatan saldo dibaca sebagai nol.

### Penerimaan barang

- Petugas memasukkan daftar baris berisi `sku` dan `quantity` dalam satu penerimaan. Satu kiriman boleh memuat beberapa kombinasi dari produk yang sama maupun beberapa produk berbeda.
- Jumlah masuk berupa bilangan bulat positif dalam satuan unit. Daftar baris harus berisi minimal satu SKU. SKU yang muncul lebih dari satu kali dalam satu kiriman dijumlahkan berdasarkan UUID varian sebelum mencatat baris.
- Saat petugas mengonfirmasi, sistem menyimpan ID penerimaan, gudang, akun pencatat, waktu, baris transaksi, dan saldo baru dalam satu transaksi. Jika satu baris gagal validasi, penolakan mencakup seluruh penerimaan dan tidak mencatat perubahan saldo.
- Penerimaan yang sudah dikonfirmasi menjadi catatan tetap. Koreksi yang disetujui masuk sebagai transaksi baru yang mengacu pada penerimaan tersebut.

### Koreksi penerimaan

- Ajukan koreksi dengan ID penerimaan awal, alasan yang wajib diisi, dan satu atau lebih baris keliru dari penerimaan tersebut. Setiap baris berisi ID baris awal dan jumlah baru yang benar. Jumlah baru berupa bilangan bulat nonnegatif. Baris yang tidak dikirim mempertahankan jumlah efektifnya.
- Hitung delta terhadap **jumlah efektif** baris setelah seluruh koreksi terdahulu. Contohnya, penerimaan 8 unit yang dikoreksi menjadi 6 menghasilkan delta −2. Jika kemudian dikoreksi menjadi 7, delta berikutnya adalah +1.
- Bila SKU awal salah, jumlah pada SKU awal dapat dikoreksi sampai nol dan SKU yang benar dicatat melalui penerimaan. Koreksi menolak baris yang bukan milik penerimaan awal serta delta yang akan membuat saldo gudang menjadi negatif. Aksi yang ditolak tidak meninggalkan koreksi atau perubahan sebagian.
- Simpan akun, waktu, alasan, rujukan penerimaan dan baris awal, jumlah efektif sebelumnya, jumlah baru, dan delta. Riwayat menampilkan penerimaan serta koreksi sesuai urutan kejadian.

### Konsistensi dan pengiriman ulang

- Saldo merupakan jumlah seluruh transaksi stok yang sudah dikonfirmasi untuk varian dan gudang. Jika saldo disimpan sebagai proyeksi, perbarui bersama catatan transaksi pada commit yang sama. Serialisasikan transaksi yang menyentuh saldo atau baris penerimaan yang sama. Koordinasikan kunci varian dengan pengecekan perubahan SKU.
- Pada POST penerimaan dan koreksi, gunakan `Idempotency-Key` yang dipertahankan oleh pemanggil saat mengulang request yang sama. Cakupan keunikan adalah tenant, akun, dan jenis operasi. Key dengan payload yang sama mengembalikan hasil transaksi pertama. Pemakaian key yang sama untuk payload atau target berbeda memberi `409`. Permintaan ulang setelah validasi gagal dapat dikirim kembali dengan data yang diperbaiki.

### Saldo dan total per opsi

- Saldo per SKU menampilkan UUID varian, kode SKU yang berlaku, nilai opsi, dan kuantitas per unit. Total produk adalah penjumlahan seluruh saldo variannya.
- Untuk setiap opsi, kelompokkan varian berdasarkan identitas nilai opsi pada varian lalu jumlahkan saldo dalam kelompok tersebut. Produk tanpa opsi memiliki satu varian bawaan dan rincian opsi kosong.
- Contoh Kaos Basic memiliki 8 unit Hitam/M, 2 unit Hitam/L, dan 5 unit Putih/L. Total produk adalah 15. Total per warna adalah Hitam 10 dan Putih 5. Total per ukuran adalah M 8 dan L 7. Setiap pengelompokan merupakan cara melihat saldo SKU yang sama.

### Ketersediaan untuk storefront

- Kontrak baca menerima tenant yang sudah dipetakan server dan sekumpulan UUID varian miliknya. Hasil per varian adalah `IN_STOCK` untuk saldo positif atau `OUT_OF_STOCK` untuk saldo nol. Varian yang tidak berada dalam himpunan milik tenant tidak boleh menghasilkan status dari tenant lain.
- Status mengikuti saldo yang sudah dikonfirmasi pada pembacaan berikutnya. Visibilitas produk ditentukan oleh status publikasi di Product Management. Storefront menampilkan label **Tersedia** atau **Habis** sesuai status yang dibaca.

### Kontrak HTTP

Gunakan `ApiResponse`, pagination repo untuk riwayat, dan `ProblemDetail` untuk kegagalan. Akun tanpa autentikasi ditolak dengan `401`. Akun tanpa tenant atau tanpa permission mendapat `403`. Target di luar tenant pada path mendapat `404`. Referensi baris atau SKU yang tidak valid di payload serta jumlah yang tidak sah mendapat `400`. Konflik payload idempoten atau koreksi yang akan membuat saldo negatif mendapat `409`.

| Metode dan path relatif | Izin | Input dan hasil sukses |
| --- | --- | --- |
| `POST /api/v1/inventory/receipts` | `inventory.receive` | `Idempotency-Key`, daftar `lines` berisi `sku` dan `quantity`. `201` dengan ID, waktu, dan baris penerimaan. |
| `GET /api/v1/inventory/receipts` | `inventory.read` | `200` dengan daftar ringkas penerimaan milik tenant dalam pagination. |
| `GET /api/v1/inventory/receipts/{receiptId}` | `inventory.read` | `200` dengan penerimaan, jumlah awal, jumlah efektif per baris, dan riwayat koreksi. |
| `POST /api/v1/inventory/receipts/{receiptId}/corrections` | `inventory.correct` | `Idempotency-Key`, `reason`, daftar `lines` berisi `receiptLineId` dan `quantity` baru. `201` dengan detail koreksi dan delta. |
| `GET /api/v1/inventory/products/{productId}/stock` | `inventory.read` | `200` dengan saldo varian, `totalQuantity`, dan total per nilai untuk setiap opsi. |

## Data Model

| Data | Kepemilikan dan integritas |
| --- | --- |
| Gudang | UUID dan `tenant_id`. Satu gudang bawaan per tenant pada versi ini. Referensi gudang disimpan bersama transaksi agar riwayat penerimaan tetap bisa dibaca saat dukungan gudang bertambah. |
| Penerimaan dan baris penerimaan | UUID penerimaan, tenant, gudang, pencatat, waktu, serta baris yang merujuk UUID varian, SKU saat penerimaan, dan jumlah positif. Pasangan penerimaan dan varian unik. |
| Koreksi dan baris koreksi | UUID koreksi, penerimaan awal, tenant dan gudang yang sama, pencatat, waktu, alasan, serta delta per baris penerimaan yang dikoreksi. |
| Saldo varian | Satu saldo untuk pasangan gudang dan varian. Gudang dan varian harus berada dalam tenant yang sama. Saldo hasil transaksi wajib nonnegatif. |
| Kunci idempotensi | Simpan identitas request dan hasil yang sudah dikonfirmasi dalam cakupan tenant, akun, serta operasi. Hubungkan kunci dan transaksi dengan batas atomik yang sama. |

## Testing Decisions

Gunakan Bruno dengan `bru run --env Local` dari direktori `bruno/`. Untuk alur penerimaan dan koreksi, baca kembali riwayat dan saldo melalui akun tenant dengan permission yang sesuai setelah aksi berhasil ataupun ditolak. Gunakan pengujian transaksi pada PostgreSQL untuk memastikan penerimaan bersamaan, retry, dan koreksi bersamaan tidak menghilangkan perubahan saldo atau menggandakan penerimaan. Konfirmasi status publik melalui skenario storefront Product Management.

## Acceptance Scenarios

| ID | Requirement | Kondisi, aksi, dan hasil |
| --- | --- | --- |
| **IM-AT-01** | IM-01, IM-02 | Toko A mulai dari saldo nol. Dalam satu penerimaan, masukkan 8 unit Hitam/M dari produk Draft, 5 unit Putih/L, dan 3 unit dari produk tanpa opsi. Dapat satu penerimaan dan saldo naik sesuai tiap SKU. Seluruh transaksi memakai gudang A. |
| **IM-AT-02** | IM-02 | Kirim penerimaan dengan satu baris sah dan satu SKU yang tidak ada dalam tenant A atau jumlah tidak valid. Dapat `400`. Setelah dibaca ulang, tidak ada penerimaan baru dan seluruh saldo tetap. |
| **IM-AT-03** | IM-02 | Ulangi POST penerimaan dengan key dan payload yang sama. Respons merujuk penerimaan awal dan saldo hanya bertambah sekali. Kirim payload berbeda dengan key itu dan periksa `409`. |
| **IM-AT-04** | IM-03 | Terima 8 unit, koreksi menjadi 6 dengan alasan, lalu menjadi 7. Riwayat memuat penerimaan dan dua koreksi dengan delta −2 dan +1. Saldo akhirnya 7. Ulangi request koreksi dengan key yang sama dan buktikan koreksi tidak digandakan. |
| **IM-AT-05** | IM-01, IM-03 | Koreksi tanpa alasan atau dengan baris dari penerimaan lain ditolak `400`. Akun tanpa izin koreksi mendapat `403`. Detail penerimaan dan saldo tidak berubah. |
| **IM-AT-06** | IM-01, IM-04 | Stok 8 Hitam/M, 2 Hitam/L, dan 5 Putih/L menampilkan total produk 15, warna 10/5, serta ukuran 8/7. Setelah Hitam/M dikoreksi menjadi 6, total menjadi 13, warna 8/5, dan ukuran 6/7. |
| **IM-AT-07** | IM-01, IM-04 | Akun B berizin mencoba membaca atau mengoreksi penerimaan A dan mendapat `404`. Riwayat B hanya memuat penerimaan B. Baca ulang saldo dan riwayat A untuk memastikan tidak ada perubahan. |
| **IM-AT-08** | IM-05 | Untuk produk terbit, varian lama dengan saldo nol berstatus Habis. Terima stok lalu baca ulang detail publik dan periksa status Tersedia. Koreksi ke nol dan periksa Habis serta produk tetap dapat dilihat. |
| **IM-AT-09** | IM-01, IM-02, IM-03 | Lakukan dua penerimaan bersamaan pada SKU yang sama, lalu dua koreksi bersamaan pada baris yang sama. Saldo mencerminkan setiap penerimaan dan delta koreksi berdasarkan jumlah efektif terakhir tanpa kehilangan transaksi. |

## Out of Scope

- **Enhancement berikutnya:** beberapa gudang per tenant, pilihan gudang pada transaksi, dan transfer stok antargudang.
- Reservasi, stok keluar karena penjualan, pengembalian, stok opname, dan integrasi pesanan atau POS.

## Further Notes

Implementasi penerimaan membutuhkan kontrak varian dan SKU yang stabil dari Product Management serta tenant dan permission dari User Management. Dukungan reservasi dapat dirancang bersama Cart, Checkout, dan Order ketika aturan pemesanan sudah dibahas. [Development Dependencies](development-dependencies.md)

## Implementation Tickets

- [Inventory 01 — Penerimaan beberapa SKU dan saldo](tickets/inventory-management/01-penerimaan-dan-saldo.md)
- [Inventory 02 — Koreksi penerimaan dan agregat stok](tickets/inventory-management/02-koreksi-dan-agregat.md)
