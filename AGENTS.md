# AI Developer Instructions — Dagantara

Berlaku untuk seluruh repository. Instruksi pada `backend/AGENTS.md` menambah aturan ketika mengerjakan backend; skill pada `.agents/skills/` dipakai sesuai jenis tugas.

## Project context

Dagantara adalah platform e-commerce multi-tenant. Backend direncanakan memakai Spring Boot; dokumentasi modul saat ini merupakan spesifikasi pengembangan, bukan bukti bahwa kode fiturnya sudah ada.

## Source of truth

1. Baca `docs/README.md` serta spesifikasi perubahan dan dokumen domain yang relevan sebelum membuat perubahan.
2. Spesifikasi perubahan yang disetujui menetapkan hasil dan cakupan pekerjaan. Dokumen domain seperti `docs/specs/user-management.md` dan `docs/specs/product-management.md` menyimpan aturan modul yang berlaku.
3. `docs/index.html` adalah viewer dokumentasi, **bukan** sumber requirement mandiri.
4. Instruksi tugas yang eksplisit dapat mengubah cakupan; bila bertentangan dengan keputusan terdokumentasi, sebutkan konfliknya dan jangan diam-diam mengubah keputusan domain.
5. Hal yang belum final tetap menjadi implementation dependency. Jangan menyamakan contoh, navigasi admin, atau usulan dengan fitur yang sudah disetujui.

## Workflow setiap tugas

1. Identifikasi spesifikasi, tiket atau ID requirement, dan kriteria penerimaan yang relevan.
2. Inspeksi struktur dan pola kode yang sudah ada; jangan berasumsi project telah ter-bootstrap.
3. Batasi perubahan ke vertical slice yang diminta. Jangan membuat endpoint, tabel, UI, atau aturan bisnis tambahan tanpa spesifikasi.
4. Tentukan dampak terhadap tenant isolation, authorization, transaksi data, dan kompatibilitas API.
5. Implementasikan dengan pola kode konsisten; jangan refactor bagian tidak terkait.
6. Jalankan verifikasi yang tersedia dan laporkan perintah, hasil, serta keterbatasannya. Jangan mengklaim pengujian lulus jika tidak dijalankan.
7. Ringkas perubahan berdasarkan file dan ID requirement. Berikan catatan jika ada keputusan domain yang masih diperlukan.

Gunakan koleksi Bruno di `bruno/` untuk pengujian API HTTP. Untuk backend lokal, jalankan `bru run --env Local` dari folder `bruno/`.

## Invarian domain yang wajib dijaga

- Data operasional tenant A tidak boleh terbaca/berubah oleh tenant B tanpa otorisasi yang terdokumentasi.
- User memiliki satu role aktif melalui `users.role_id`; permission diturunkan dari `roles -> role_permissions -> permissions`.
- Akun platform dapat memiliki `users.tenant_id = NULL`; ini bukan alasan otomatis untuk bypass semua izin.
- Setiap tenant memiliki **Independent Catalog**, bukan katalog produk bersama.
- Produk dapat memiliki beberapa kategori milik tenant yang sama. Tepat satu Primary Category disimpan pada `products.primary_category_id` dan harus termasuk dalam `product_categories` produk itu.
- Media tetap dimiliki produk; relasi `product_media <-> product_option_values` many-to-many melalui `media_option_values`.
- Inventory mencatat saldo berdasarkan identitas varian dalam gudang tenant. Versi pertama mempunyai satu gudang per tenant dan SKU unik dalam tenant.
- Nama menu/submenu pada `Admin UI Structure` tidak otomatis mengotorisasi implementasi API atau fitur baru.

## Skill routing

| Ketika tugas menyangkut | Baca |
| --- | --- |
| Membahas ide, menulis spesifikasi perubahan, atau mengusulkan tiket | `.agents/skills/write-module-spec/SKILL.md` |
| Mendesain layout, halaman, alur UI, atau prototipe untuk ditinjau | `.agents/skills/design-ui/SKILL.md` |
| Mengimplementasikan spesifikasi, tiket, atau skenario | `.agents/skills/implement-from-spec/SKILL.md` |
| Endpoint REST, DTO, validasi request, response/error contract | `.agents/skills/spring-boot-api/SKILL.md` |
| Membuat atau menyiapkan database PostgreSQL lokal Dagantara di Windows | `.agents/skills/create-postgres-database/SKILL.md` |
| JPA, PostgreSQL, relasi, schema/migration, query | `.agents/skills/spring-boot-database/SKILL.md` |
| Autentikasi, JWT, role/permission, tenant access | `.agents/skills/spring-boot-security/SKILL.md` |
| Unit, MVC, integration, repository, atau security test Spring Boot | `.agents/skills/spring-boot-testing/SKILL.md` |
| Smoke test dan skenario penerimaan API HTTP melalui Bruno | `.agents/skills/bruno-api-testing/SKILL.md` |
| Menjalankan atau memeriksa backend lokal | `.agents/skills/spring-boot-run/SKILL.md` |

Gunakan beberapa skill sekaligus jika cakupannya saling bersinggungan. Jangan menganggap skill sebagai requirement fitur baru.

Gunakan kembali isi file yang sudah tersedia dalam konteks tugas. Buka bagian yang relevan jika file berubah atau pekerjaan membutuhkan informasi yang belum tersedia di konteks.

## Quality gate

- Tidak ada rahasia, kredensial, token, atau PII yang dicetak ke log/commit.
- Jangan menimpa konfigurasi atau keputusan yang sudah ada tanpa alasan yang jelas.
- Jaga perubahan kecil dan mudah ditinjau; satu perubahan logis per commit bila memungkinkan.
