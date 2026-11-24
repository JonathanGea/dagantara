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
- [Inventory Management](specs/inventory-management.md)
- [Store Settings — Identitas Publik Toko](specs/store-settings.md)
- [Product Catalog / Discovery — Pencarian dan Filter Produk Publik](specs/product-discovery.md)

## Penggunaan dalam Pengembangan

- [Aturan kerja repository](../AGENTS.md) memuat sumber requirement, invarian domain, dan alur verifikasi.
- [Panduan Penggunaan Skill](guides/skill-guide.md) memuat alur pembahasan, desain, implementasi, dan contoh prompt.

## Regenerasi viewer

Setelah mengubah skill atau dokumen yang tampil di viewer, jalankan `powershell -ExecutionPolicy Bypass -File .\docs\viewer\generate-skills.ps1` dari root repository. Skrip memperbarui salinan publik skill dan data yang tertanam di `index.html`. Untuk halaman Markdown baru, tambahkan entri di indeks ini dan blok `data-doc` di viewer agar halaman juga tersedia saat HTML dibuka langsung.
