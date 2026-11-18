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
- [Product Management](product-management.md)

## Kesepakatan dokumentasi

### Isi dokumentasi

- Cantumkan **hanya keputusan yang telah disepakati**. Jangan mencatat fitur atau alternatif yang tidak dipilih.
- Tulis aturan sekali, secara singkat, jelas, dan konsisten. Gunakan judul langsung pada fungsi yang dibahas.
- Setiap modul memiliki file Markdown sendiri. `README.md` menjadi indeks; `index.html` menampilkan file Markdown.
- Sertakan **Database ERD**, **Business Rules & Validation**, dan **Tenant Admin — Menu & Features** sesuai keputusan modul.
- **Database ERD** memuat diagram Mermaid, fungsi setiap tabel, serta penjelasan relasi dan constraint yang disepakati. Gunakan PK/FK yang ada pada model; jangan menambahkan kolom atau mekanisme penyimpanan yang belum diputuskan.
- Pada bagian **Tenant Admin — Menu & Features**, gambarkan hierarki **Tenant Admin → menu → submenu** seperti struktur sidebar, lalu jelaskan halaman, fitur, dan aksi yang **sudah disepakati**. Nama menu/submenu yang tercantum pada referensi desain dapat digunakan sebagai peta navigasi; detail fungsi yang belum disepakati jangan direkayasa. Satu aksi tidak harus menjadi submenu.
- Tambahkan **Permissions**, **Lifecycle / Workflow**, dan **Cross-Module Relationships** hanya bila aturannya relevan dan telah disepakati.
- Gunakan Mermaid untuk memperlihatkan struktur atau alur yang memang membutuhkan diagram.
- Jangan mengisi kolom, permission code, kontrak API, status implementasi, atau struktur menu dengan asumsi. Jangan membuat section kosong hanya demi memenuhi template.
- Saat keputusan berubah, perbarui dokumen modul dan diagram yang terdampak; jangan mengubah modul lain tanpa kebutuhan.
