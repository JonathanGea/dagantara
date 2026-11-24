# Dagantara

Dagantara adalah platform e-commerce multi-tenant. Setiap tenant beroperasi dalam ruang data miliknya sendiri.

## Dokumentasi Modul

- [Progres Modul](planning/progress.md)
- [Module Roadmap & Ownership](planning/module-roadmap.md)
- [Development Dependencies & Parallel Workstreams](planning/development-dependencies.md)

## Spesifikasi Perubahan

Spesifikasi yang disepakati ditautkan di bagian ini. Judul atau nama file tanpa ekstensi dapat dipakai untuk memilih spec.

- [User Management — Akses Platform dan Tenant](specs/user-management.md)
- [Product Management](specs/product-management.md)
- [Tenant Management — Penonaktifan dan Aktivasi Kembali Tenant](specs/tenant-management.md)
- [Inventory Management](specs/inventory-management.md)
- [Store Settings — Identitas Publik Toko](specs/store-settings.md)
- [Product Catalog / Discovery — Pencarian dan Filter Produk Publik](specs/product-discovery.md)

Draf spec untuk modul yang menunggu pembahasan ditautkan dari [Progres Modul](planning/progress.md).

## Penggunaan dalam Pengembangan

- [Aturan kerja repository](../AGENTS.md) memuat sumber requirement, invarian domain, dan alur verifikasi.
- [Panduan Penggunaan Skill](guides/skill-guide.md) memuat alur pembahasan, desain, implementasi, dan contoh prompt.

## Regenerasi viewer

Setelah mengubah skill, dokumen yang tampil di viewer, atau daftar dan judul tiket, jalankan `powershell -ExecutionPolicy Bypass -File .\docs\viewer\generate-skills.ps1` dari root repository. Skrip menanamkan skill dari `.agents/skills/`, daftar tiket dari `docs/tickets/`, dan salinan dokumen di `index.html`. Viewer membaca tiket dan draf spec dari file Markdown masing-masing. Untuk halaman baru yang akan tampil di indeks, tambahkan tautan di sini dan blok `data-doc` di viewer agar halaman juga tersedia saat HTML dibuka langsung.
