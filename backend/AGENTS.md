# Backend Agent Instructions — Dagantara

Berlaku untuk `backend/**`. Baca juga `../AGENTS.md` dan modul relevan pada `../docs/` sebelum mengubah backend.

## Scope dan pendekatan

- Fokus pada Spring Boot API, domain logic, persistence, security, dan testing. Tidak mengimplementasikan fitur yang baru muncul sebagai label navigasi.
- Pertahankan versi Java, Spring Boot, build tool, package naming, convention, dan dependency yang benar-benar ada di repository. Jangan menentukan versi/arsitektur tambahan saat backend masih berupa skeleton.
- Saat kode belum tersedia, siapkan hanya hal yang diminta. Jangan membuat scaffold aplikasi penuh secara implisit.
- Pisahkan tanggung jawab: controller untuk HTTP, service untuk use case/transaction, repository untuk persistence, DTO untuk kontrak I/O, entity untuk persistence.
- Validasi input di boundary dan invariannya di service; jangan bergantung hanya pada kontrol di frontend.
- Batasi transaksi pada unit kerja yang perlu konsistensi. Hindari N+1, lazy loading di luar transaksi, dan akses entity langsung dari response publik.

## Struktur package modul

Kelompokkan kode berdasarkan modul, lalu tanggung jawab di dalam modul. Contoh struktur di bawah `backend/src/main/java/com/dagantara/backend`.

```text
modules/<nama_module>/
├── controller/   # endpoint HTTP
├── service/      # use case, transaksi, dan aturan modul
├── dto/          # request dan response
├── repository/   # akses data
└── entity/       # entity persistence
```

Buat subpackage yang dibutuhkan modul. Tempatkan tipe request dan response di `dto` agar service dapat menggunakannya tanpa bergantung pada controller. Letakkan infrastruktur lintas modul di `common` sesuai tanggung jawabnya. Saat memindahkan kelas, sesuaikan deklarasi package dan import.

## Tenant isolation dan keamanan

- Tenant untuk operasi tenant-scoped harus berasal dari konteks terautentikasi/terverifikasi, bukan sekadar `tenantId` yang dikirim client.
- Validasi kepemilikan tenant pada read, create, update, delete, relasi antar-entitas, dan query teragregasi.
- Role/permission mengikuti `docs/specs/user-management.md`: user memiliki satu role aktif, permission diambil lewat relasi role. Jangan mengandalkan nama role sebagai pengganti permission.
- Akun platform dan role sistem dapat mempunyai `tenant_id = NULL`; akses lintas tenant tetap memerlukan aturan otorisasi yang eksplisit.
- Jangan menambahkan bypass keamanan, hardcoded secret, atau logging data sensitif.

## Ketergantungan keputusan domain

- Produk adalah milik satu tenant (Independent Catalog).
- Hubungan Product–Category many-to-many. Gunakan `products.primary_category_id` dan constraint ke relasi `product_categories` sesuai PM-02.
- Media harus dimiliki produk dan asosiasinya dengan nilai opsi adalah many-to-many (PM-03/PM-04).
- Penerimaan dan koreksi stok memakai UUID varian, gudang dan tenant yang sesuai, serta satu transaksi untuk seluruh perubahan saldo. Ikuti `../docs/specs/inventory-management.md`.
- Struktur tabel yang belum ditetapkan detailnya tidak boleh dianggap final hanya dari gambar referensi.

## Checklist pengerjaan

1. Petakan task ke ID di `../docs/specs/user-management.md`, `../docs/specs/product-management.md`, atau `../docs/specs/inventory-management.md`.
2. Pilih skill yang sesuai di `../.agents/skills/`; security dan testing sering juga berlaku pada tugas API/database.
3. Baca kode yang terdampak dan tes yang sudah ada.
4. Implementasikan change set kecil dengan validasi tenant, permission, dan integritas data.
5. Jalankan test/build yang tersedia. Jika belum ada `pom.xml` / `build.gradle` atau test suite, laporkan bahwa verifikasi belum dapat dijalankan.
6. Sebutkan file yang berubah, acceptance scenario yang telah diverifikasi, dan keputusan yang masih blocked.
