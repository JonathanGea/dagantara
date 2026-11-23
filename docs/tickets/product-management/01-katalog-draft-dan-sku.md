# Product 01 — Katalog Draft, kategori, opsi, dan SKU

Spec induk: [Product Management](../../product-management.md)

Status: ready-for-agent

## What to Build

Implementasikan API admin kategori, produk Draft, detail, opsi, dan varian untuk PM-01, PM-02, PM-05, dan PM-06. Daftarkan permission katalog sesuai User Management. Sediakan kontrak varian untuk Inventory 01, termasuk lookup SKU terisolasi tenant dan koordinasi kunci varian untuk serialisasi perubahan SKU dan pencatatan stok.

## Acceptance Criteria

- [ ] Admin tenant dengan `catalog.manage` bisa membuat kategori dan produk Draft serta memperbarui detail, kategori, dan nilai opsi sesuai kontrak. Akun dengan `catalog.read` bisa membaca daftar dan detail. Pagination hanya mencakup tenant pelaku.
- [ ] PM-AT-02 — Buat dan ganti kategori beserta Primary Category dalam satu transaksi. Primary Category wajib menjadi anggota daftar kategori. Referensi kategori tenant lain atau primary di luar daftar menghasilkan `400` dan kategori sebelumnya tetap tersimpan. Buktikan constraint PostgreSQL.
- [ ] PM-AT-05 — Simpan varian AUTO dan MANUAL, dengan UUID stabil, pola dan normalisasi SKU yang ditentukan, serta varian bawaan untuk produk tanpa opsi. Duplikasi SKU dalam tenant atau kombinasi memberi `409`. SKU yang sama bisa digunakan oleh tenant lain. Pembaruan label mempertahankan ID nilai dan SKU tersimpan.
- [ ] Bagian katalog PM-AT-01 dan PM-AT-09 — Daftar B hanya menampilkan katalog B, detail produk A dari akun B memberi `404`, perubahan oleh akun baca-saja memberi `403`, dan request admin tanpa login memberi `401`. Baca ulang data setelah penolakan.
- [ ] Bagian awal PM-AT-06 — Perbaiki SKU sebelum penerimaan pertama tanpa mengganti ID varian. Inventarisasi dan uji kontrak kunci bersama. Inventory 01 memverifikasi penolakan perubahan SKU setelah penerimaan.
- [ ] Jalankan skenario Bruno yang relevan dan pengujian constraint PostgreSQL.

## Blocked By

- [User Management](../../user-management.md#implementation-tickets) untuk login, identitas tenant, dan permission role aktif.
