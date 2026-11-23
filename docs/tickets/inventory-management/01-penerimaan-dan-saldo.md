# Inventory 01 — Penerimaan beberapa SKU dan saldo

Spec induk: [Inventory Management](../../inventory-management.md)

Status: ready-for-agent

## What to Build

Implementasikan IM-01, IM-02, riwayat dan saldo per SKU dari IM-04, serta kontrak baca IM-05. Buat gudang bawaan saat penerimaan pertama berhasil. Penerimaan menulis riwayat dan saldo secara atomik serta mempertahankan penguncian SKU. Inventory 02 melengkapi koreksi dan agregat.

## Acceptance Criteria

- [ ] IM-AT-01 — Terima beberapa SKU, termasuk varian produk Draft dan tanpa opsi, dalam satu penerimaan dan gudang tenant. Gabungkan SKU berulang berdasarkan UUID varian. Baca saldo awal nol, saldo baru per SKU, dan riwayat penerimaan melalui API.
- [ ] IM-AT-02 — Tolak keseluruhan penerimaan dengan `400` jika satu SKU di luar tenant atau jumlah tidak valid. Baca ulang riwayat dan saldo untuk membuktikan penolakan atomik.
- [ ] IM-AT-03 — Ulangi key dan payload yang sama, lalu verifikasi ID penerimaan dan saldo yang hanya bertambah sekali. Payload berbeda dengan key sama menghasilkan `409`. Key dapat dipakai kembali setelah validasi gagal.
- [ ] Bagian baca IM-AT-07 — Riwayat tenant B mengecualikan penerimaan A dan detail penerimaan A memberi `404`. Request tanpa login memberi `401` dan akun tanpa izin memberi `403`.
- [ ] PM-AT-06 — Terima stok berdasarkan SKU yang telah diperbaiki. Perubahan SKU berikutnya ditolak `409` dan stok tetap terhubung ke UUID yang sama. Buktikan serialisasi yang mencegah perubahan SKU berlomba dengan penerimaan.
- [ ] IM-05 dan bagian penerimaan IM-AT-09 — Kontrak baca membatasi hasil ke varian milik tenant, memetakan saldo nol dan positif, serta melihat transaksi setelah commit. Penerimaan paralel dan retry pada PostgreSQL mempertahankan setiap penambahan tanpa catatan ganda.
- [ ] Jalankan skenario Bruno yang relevan dan uji transaksi PostgreSQL.

## Blocked By

- [Product 01](../product-management/01-katalog-draft-dan-sku.md) untuk UUID varian, lookup SKU, dan mekanisme koordinasi transaksi yang menyentuh varian.
- [User Management](../../user-management.md#implementation-tickets) untuk login, identitas tenant, dan permission role aktif.
