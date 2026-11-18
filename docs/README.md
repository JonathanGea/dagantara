# Dagantara

> **Developer Documentation** · E-commerce multi-tenant platform

Dokumentasi teknis Dagantara.

## Gambaran umum

Dagantara adalah fondasi e-commerce **multi-tenant**. Satu platform mengelola banyak tenant (bisnis/toko), tetapi setiap tenant hanya mengakses data yang berada dalam ruang lingkupnya sendiri.

Ada dua lapisan operasional utama:

- **Platform** — mengelola ekosistem tenant dan administrator tingkat platform.
- **Tenant** — mengelola pengguna dan operasional bisnis di dalam tenant masing-masing.

## Dokumentasi

- [User Management](user-management.md)

## Kesepakatan dokumentasi

Setiap pembahasan disimpan dalam **file Markdown terpisah sesuai modul**. `README.md` berfungsi sebagai indeks tautan dokumen; `index.html` hanya menampilkan isi file Markdown, bukan menjadi tempat menyimpan materi.
