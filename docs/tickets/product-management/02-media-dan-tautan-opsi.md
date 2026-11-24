# Product 02 — Media dan tautan nilai opsi

Spec induk: [Product Management](../../specs/product-management.md)

Status: done

## What to Build

Implementasikan PM-03 dan PM-04. Admin menyimpan URL foto dan mengganti tautan nilai opsi per media secara atomik. Detail admin menyertakan daftar media terurut dan `optionValueIds` untuk tiap media.

## Acceptance Criteria

- [x] PM-AT-03 — Tambahkan URL HTTP atau HTTPS ke produk lalu tautkan satu media ke dua nilai opsi produk itu. Detail mengembalikan kedua relasi. Nilai opsi produk lain menghasilkan `400` dan tautan awal tetap tersimpan.
- [x] PM-AT-04 — Tautkan dua media ke nilai opsi yang sama. Penggantian dengan daftar kosong menghapus tautan hanya untuk media target. Detail mengembalikan media dalam urutan waktu penambahan dan ID.
- [x] Bagian media PM-AT-01 — Akun pengelola B yang mencoba menautkan media produk A menerima `404`. Akun A membaca kembali detail dan melihat tautan yang sama. Aksi tulis memerlukan `catalog.manage`.
- [x] Jalankan skenario Bruno yang relevan dan uji integritas relasi PostgreSQL.

## Blocked By

- [Product 01](01-katalog-draft-dan-sku.md) untuk produk, opsi, permission katalog, dan detail admin.

## Kontrak media dan galeri

- `POST /api/v1/catalog/products/{productId}/media` menerima `{"url": "..."}`. Respons `201` berisi `data` dengan `id`, `url`, dan `optionValueIds: []`.
- `PUT /api/v1/catalog/products/{productId}/media/{mediaId}/option-values` mengganti seluruh himpunan `optionValueIds` dan mengembalikan bentuk `data` yang sama dengan status `200`. Daftar kosong hanya menghapus tautan pada media target. Kedua aksi memakai `catalog.manage`. Target di luar tenant atau produk pada URL memberi `404`, sedangkan URL media atau nilai opsi pada body yang tidak valid memberi `400`.
- `GET /api/v1/catalog/products/{productId}` memakai `catalog.read` dan menyertakan `data.media`. Untuk Product 03, `CatalogMediaRepository.listForProduct(UUID tenantId, UUID productId)` mengembalikan `List<CatalogResponses.Media>` dalam urutan `created_at, id`, dengan URL dan ID opsi di tiap entri. Pemanggil storefront menggunakan hasil resolusi tenant dari URL toko dan memastikan produk sudah terbit sebelum membaca galeri.
- Terapkan `07-product-media.sql` setelah `06-product-catalog.sql` dari commit Product 01 `349dddf`.

## Verifikasi lokal

Jalankan `mvnw.cmd package` dari `backend/` dengan JDK 25 pada `JAVA_HOME` dan set `PRODUCT01_TEST_DB_URL`, `PRODUCT01_TEST_DB_USERNAME`, serta `PRODUCT01_TEST_DB_PASSWORD` untuk suite `CatalogPostgresTest`. Suite membuat dan menghapus skema uji unik.

Untuk Bruno, siapkan database disposable dengan migrasi 01–07, set `PRODUCT02_FIXTURE_HOST` dan `PRODUCT02_FIXTURE_PASSWORD`, lalu jalankan `fixture.sql` di folder skenario menggunakan `psql -X -v ON_ERROR_STOP=1`. Fixture membuat dua tenant dan akun baca-saja. Gunakan password uji yang sama pada proses Bruno. Setelah selesai, hapus database disposable untuk membersihkan seluruh data fixture. Jalankan dari `bruno/` dan sesuaikan override `baseUrl` dengan backend fixture.

```text
bru run smoke --env Local --env-var baseUrl=http://localhost:18092 --bail --noproxy --sandbox developer
bru run product-management/02-media-dan-tautan-opsi --env Local --env-var baseUrl=http://localhost:18092 --bail --noproxy --sandbox developer
```

| Pemeriksaan | Hasil |
| --- | --- |
| Smoke Bruno | Lulus, 1 request dan 2 tes. |
| PM-AT-01 bagian media, PM-AT-03, PM-AT-04 | Lulus, 30 request Bruno. Pembacaan ulang memverifikasi keadaan setelah penolakan dan setelah penghapusan tautan. |
| PostgreSQL 16 dan Maven | Lulus, 9 tes. Tiga kasus media memverifikasi FK lintas kepemilikan, keunikan, rollback, urutan saat timestamp sama, dan penggantian bersamaan. |
