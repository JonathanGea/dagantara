# Product 02 — Media dan tautan nilai opsi

Spec induk: [Product Management](../../product-management.md)

Status: ready-for-agent

## What to Build

Implementasikan PM-03 dan PM-04. Admin menyimpan URL foto dan mengganti tautan nilai opsi per media secara atomik. Detail admin menyertakan daftar media terurut dan `optionValueIds` untuk tiap media.

## Acceptance Criteria

- [ ] PM-AT-03 — Tambahkan URL HTTP atau HTTPS ke produk lalu tautkan satu media ke dua nilai opsi produk itu. Detail mengembalikan kedua relasi. Nilai opsi produk lain menghasilkan `400` dan tautan awal tetap tersimpan.
- [ ] PM-AT-04 — Tautkan dua media ke nilai opsi yang sama. Penggantian dengan daftar kosong menghapus tautan hanya untuk media target. Detail mengembalikan media dalam urutan waktu penambahan dan ID.
- [ ] Bagian media PM-AT-01 — Akun pengelola B yang mencoba menautkan media produk A menerima `404`. Akun A membaca kembali detail dan melihat tautan yang sama. Aksi tulis memerlukan `catalog.manage`.
- [ ] Jalankan skenario Bruno yang relevan dan uji integritas relasi PostgreSQL.

## Blocked By

- [Product 01](01-katalog-draft-dan-sku.md) untuk produk, opsi, permission katalog, dan detail admin.
