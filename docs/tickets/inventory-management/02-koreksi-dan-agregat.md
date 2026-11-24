# Inventory 02 — Koreksi penerimaan dan agregat stok

Spec induk: [Inventory Management](../../inventory-management.md)

Status: done

## What to Build

Implementasikan IM-03 dan lengkapi IM-04. Catat koreksi sebagai transaksi yang mengacu pada penerimaan dan baris awal. Tampilkan jumlah efektif, riwayat koreksi, total produk, dan total per nilai opsi dari saldo varian.

## Acceptance Criteria

- [x] IM-AT-04 — Koreksi penerimaan 8 menjadi 6 lalu 7. Baca penerimaan, alasan, pencatat, jumlah efektif, delta −2 dan +1, serta saldo akhir 7. Retry identik idempoten, dan key yang sama untuk payload atau target berbeda memberi `409`.
- [x] IM-AT-05 — Tolak alasan kosong atau baris dari penerimaan lain dengan `400`, akun tanpa `inventory.correct` dengan `403`, dan saldo negatif dengan `409`. Riwayat dan saldo tetap sama sesudah penolakan.
- [x] IM-AT-06 — Dari saldo 8, 2, dan 5, verifikasi total produk 15, warna 10/5, dan ukuran 8/7. Setelah koreksi 8 menjadi 6, hasilnya 13, 8/5, dan 6/7. Produk tanpa opsi menampilkan rincian opsi kosong.
- [x] Bagian koreksi IM-AT-07 — Koreksi lintas tenant memberi `404` dan data tenant pemilik tetap sama.
- [x] Bagian Inventory dari IM-AT-08 — Koreksi ke nol mengubah hasil kontrak ketersediaan menjadi `OUT_OF_STOCK` setelah commit dan mempertahankan riwayat transaksi. Efek publik lulus dalam [verifikasi gabungan di browser](../product-management/03-publikasi-dan-storefront.md#verifikasi-gabungan-product-dan-inventory).
- [x] Bagian koreksi IM-AT-09 — Dua koreksi bersamaan berangkat dari jumlah efektif terakhir tanpa kehilangan delta.
- [x] Jalankan skenario Bruno yang relevan dan uji transaksi PostgreSQL.

## Verifikasi Inventory 02

Gunakan database fixture PostgreSQL terisolasi. Migrasi `09-inventory-corrections.sql` diterapkan setelah 08. `InventoryPostgresTest` membuat dan membersihkan schema sendiri. Dari `backend/`, jalankan dengan JDK 25 dan variabel koneksi `INVENTORY01_TEST_DB_URL`, `INVENTORY01_TEST_DB_USERNAME`, dan `INVENTORY01_TEST_DB_PASSWORD`. Gunakan variabel `PRODUCT01_TEST_DB_*` untuk ikut menjalankan regresi katalog di schema terpisah.

```powershell
.\mvnw.cmd -B package
```

Untuk uji HTTP, muat `fixture.sql` dari folder Inventory 02 dengan `psql -X -v ON_ERROR_STOP=1`. Pakai prefix fixture baru dan password uji melalui `INVENTORY02_FIXTURE_PREFIX` dan `INVENTORY02_FIXTURE_PASSWORD`. Jalankan dari `bruno/` pada backend fixture.

```powershell
bru run smoke --env Local --env-var baseUrl=http://localhost:18088 --bail --noproxy --sandbox developer
bru run inventory-management/02-koreksi-dan-agregat --env Local --env-var baseUrl=http://localhost:18088 --bail --noproxy --sandbox developer
```

Maven lulus 23 tes PostgreSQL, termasuk 14 tes Inventory dan 9 tes katalog. Bruno lulus 1 request smoke dan 35 request Inventory 02, termasuk dua POST koreksi serentak yang dikirim oleh satu skenario. PostgreSQL mengonfirmasi penantian transaksi bersamaan, rollback saat saldo tidak mencukupi, dan perubahan status kontrak batch setelah commit.

## Blocked By

- [Inventory 01](01-penerimaan-dan-saldo.md) untuk penerimaan, riwayat, efek saldo, dan UUID varian dari Product 01.
