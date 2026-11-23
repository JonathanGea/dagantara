# Inventory 02 — Koreksi penerimaan dan agregat stok

Spec induk: [Inventory Management](../../inventory-management.md)

Status: ready-for-agent

## What to Build

Implementasikan IM-03 dan lengkapi IM-04. Catat koreksi sebagai transaksi yang mengacu pada penerimaan dan baris awal. Tampilkan jumlah efektif, riwayat koreksi, total produk, dan total per nilai opsi dari saldo varian.

## Acceptance Criteria

- [ ] IM-AT-04 — Koreksi penerimaan 8 menjadi 6 lalu 7. Baca penerimaan, alasan, pencatat, jumlah efektif, delta −2 dan +1, serta saldo akhir 7. Retry identik idempoten, dan key yang sama untuk payload atau target berbeda memberi `409`.
- [ ] IM-AT-05 — Tolak alasan kosong atau baris dari penerimaan lain dengan `400`, akun tanpa `inventory.correct` dengan `403`, dan saldo negatif dengan `409`. Riwayat dan saldo tetap sama sesudah penolakan.
- [ ] IM-AT-06 — Dari saldo 8, 2, dan 5, verifikasi total produk 15, warna 10/5, dan ukuran 8/7. Setelah koreksi 8 menjadi 6, hasilnya 13, 8/5, dan 6/7. Produk tanpa opsi menampilkan rincian opsi kosong.
- [ ] Bagian koreksi IM-AT-07 — Koreksi lintas tenant memberi `404` dan data tenant pemilik tetap sama.
- [ ] Bagian koreksi IM-AT-08 dan IM-AT-09 — Koreksi ke nol mengubah hasil kontrak ketersediaan dan tetap mempertahankan riwayat transaksi. Dua koreksi bersamaan berangkat dari jumlah efektif terakhir tanpa kehilangan delta. Koordinator memverifikasi efek publik setelah Product 03 tersedia.
- [ ] Jalankan skenario Bruno yang relevan dan uji transaksi PostgreSQL.

## Blocked By

- [Inventory 01](01-penerimaan-dan-saldo.md) untuk penerimaan, riwayat, efek saldo, dan UUID varian dari Product 01.
