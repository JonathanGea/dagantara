# Product 03 — Publikasi dan storefront tenant

Spec induk: [Product Management](../../product-management.md)

Status: done

## What to Build

Implementasikan PM-07 dan PM-08. Admin mengubah status publikasi melalui API. Pengunjung membuka daftar dan detail publik pada URL toko, memilih nilai opsi, serta melihat galeri dan status ketersediaan. Gunakan pemetaan hostname atau hostname dan jalur toko dari server, serta baca ketersediaan berdasarkan UUID varian.

## Acceptance Criteria

- [x] PM-AT-07 — Draft tersembunyi pada daftar dan URL detail publik. Publikasi tanpa harga atau varian memberi `409`. Produk yang memenuhi syarat tampil setelah terbit, hilang saat ditarik, dan tampil lagi setelah diterbitkan ulang. Status yang dikirim ulang menghasilkan keadaan yang sama.
- [x] PM-AT-08 — Kartu menampilkan nama, harga rupiah dan foto pertama bila ada. Detail menampilkan galeri terurut, opsi, dan kombinasi terdaftar. Galeri mengikuti nilai terpilih pertama yang memiliki media menurut urutan opsi, dengan galeri awal sebagai fallback.
- [x] PM-AT-08 dan bagian publik IM-AT-08 — Kombinasi terdaftar menampilkan Tersedia atau Habis berdasarkan saldo yang sudah dikonfirmasi. Pilihan belum lengkap dan kombinasi yang tidak disediakan memberi hasil berbeda. Produk habis tetap tampil. Produk tanpa opsi memakai varian bawaan.
- [x] PM-AT-08 — Daftar dan detail hanya berisi produk terbit dari URL toko yang dipetakan. ID produk A pada URL B, URL yang tidak dipetakan, dan produk Draft memberi `404` yang setara. Verifikasi pemetaan domain dan jalur toko.
- [x] Bagian publikasi PM-AT-09 — Akun baca-saja bisa membaca detail admin dan perubahan publikasi mendapat `403`. Request perubahan tanpa autentikasi mendapat `401`. Baca ulang status setelah penolakan.
- [x] Jalankan skenario Bruno dan buka halaman storefront di browser. Uji alur gabungan Draft, penerimaan, terbit, pilihan galeri, serta perubahan status stok. Bagian koreksi IM-AT-08 diverifikasi setelah Inventory 02 terintegrasi.

## Blocked By

- [Product 01](01-katalog-draft-dan-sku.md) dan [Product 02](02-media-dan-tautan-opsi.md) untuk katalog, detail, dan galeri.
- [Inventory 01](../inventory-management/01-penerimaan-dan-saldo.md) untuk kontrak status per varian.
- Pemetaan URL toko aktif dari [User Management](../../user-management.md#login-dan-konteks-tenant), tersedia melalui pemetaan hostname dan jalur toko yang diprovisikan. Verifikasi prasyarat sebelum dispatch.

## Verifikasi lokal

Gunakan JDK 25 dan PostgreSQL terisolasi. Terapkan migrasi 01–08, ekspor `PRODUCT03_FIXTURE_PASSWORD` untuk akun uji, lalu terapkan `bruno/product-management/03-publikasi-dan-storefront/fixture.sql` dengan `psql -X -v ON_ERROR_STOP=1` pada database yang sama. Jalankan backend dengan `APP_PLATFORM_HOST=localhost` dan port fixture, misalnya `18083`. Fixture memetakan tenant berjalur ke `localhost/pm03` dan tenant berdomain langsung ke `127.0.0.1`. Untuk tes Spring, set `PRODUCT03_TEST_DB_URL`, `PRODUCT03_TEST_DB_USERNAME`, dan `PRODUCT03_TEST_DB_PASSWORD`. Tes membuat skema sementara dan memulai server pada port acak.

Dari `backend/`, jalankan `.\mvnw.cmd test`. Dari `bruno/`, jalankan:

```powershell
bru run smoke --env Local --env-var baseUrl=http://localhost:18083
bru run product-management/03-publikasi-dan-storefront --env Local --env-var baseUrl=http://localhost:18083 --sandbox=developer --bail
```

Pada fixture PostgreSQL, `.\mvnw.cmd package` lulus 21 tes dengan konfigurasi `PRODUCT01_TEST_DB_*`, `PRODUCT03_TEST_DB_*`, dan `INVENTORY01_TEST_DB_*`. Bruno smoke lulus 2 tes dan skenario Product 03 lulus. Orca menampilkan galeri serta pesan pilihan yang sesuai, detail Draft menghasilkan `404`, dan label varian bawaan berubah dari **Habis** ke **Tersedia** setelah penerimaan. Verifikasi browser untuk koreksi ke nol menunggu integrasi Inventory 02.

Setelah Bruno lulus, buka `http://localhost:18083/pm03/products` dan pilih kartu **Kaos** dari run tersebut. Galeri awal menampilkan kedua foto. Pilih hanya ukuran **M** untuk melihat foto ukuran dan pesan **Lengkapi pilihan produk**. Tambahkan **Hitam** untuk melihat galeri warna dan status **Tersedia**. Kombinasi **Hitam / L** menampilkan **Kombinasi tidak disediakan**. Pilih **Putih / L** untuk melihat fallback kedua foto dan status **Tersedia** setelah penerimaan. Di `http://127.0.0.1:18083/products`, buka kartu **Tas** untuk memeriksa status **Habis** yang langsung tampil dari varian bawaan.

Untuk mengulang transisi di browser, gunakan akun fixture `pm03-manager` pada URL toko. Set status Kaos ke `DRAFT` melalui endpoint publikasi, muat ulang daftar dan tautan detail, lalu terbitkan ulang. Setelah Inventory 02 terintegrasi, koreksi baris penerimaan **Putih / L** menjadi nol. Muat ulang detail, pilih kembali **Putih / L**, dan periksa **Habis** serta kartu Kaos di daftar. Hapus database fixture untuk membersihkan data uji.
