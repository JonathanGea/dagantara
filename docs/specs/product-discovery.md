# Product Catalog / Discovery — Pencarian dan Filter Produk Publik

## Problem Statement

Pengunjung yang sudah tahu nama atau kategori produk harus menyusuri daftar produk publik toko halaman demi halaman. Mereka perlu mempersempit daftar sambil tetap melihat produk yang terbit di toko yang sedang dibuka.

## Solution

Daftar produk publik menerima pencarian nama dan satu filter kategori. Pengunjung dapat menggunakan salah satu atau keduanya. Versi pertama memperluas API daftar yang sudah tersedia. Perilaku halaman `/products` dan skenario browser di bawah menjadi acuan untuk tahap frontend berikutnya.

## User Stories

1. **PD-01** — Sebagai pengunjung, saya bisa mencari potongan nama produk agar mudah menemukan produk meski hanya ingat sebagian namanya.
2. **PD-02** — Sebagai pengunjung, saya bisa memilih satu kategori agar dapat menemukan semua produk terbit yang dikelompokkan ke kategori itu, termasuk produk yang memakai kategori tersebut sebagai kategori sekunder.
3. **PD-03** — Sebagai pengunjung, saya bisa menggabungkan pencarian dan kategori serta berpindah halaman dengan kriteria yang sama agar dapat mempersempit daftar dan melanjutkan penelusuran.
4. **PD-04** — Sebagai pengunjung, saya melihat hasil dari toko yang saya buka dan bisa melihat detail produk terbit yang habis agar saya tahu ketersediaannya.
5. **PD-05** — Sebagai pengunjung, saya bisa memperbaiki pencarian atau menghapus filter saat tidak ada produk yang cocok agar bisa melanjutkan penelusuran.

## Implementation Decisions

### Daftar dan kontrak API

Gunakan `GET /api/v1/storefront/products` relatif terhadap URL toko. Pertahankan bentuk `ApiResponse`, kartu produk, dan metadata pagination. Tambahkan dua parameter query opsional.

| Parameter | Perilaku |
| --- | --- |
| `q` | Pangkas spasi di awal dan akhir. Nilai kosong setelah dipangkas setara dengan menghilangkan parameter. Cocokkan teks literal yang berurutan di posisi mana pun dalam nama produk, dengan mengabaikan kapitalisasi. |
| `categoryId` | Satu UUID kategori. Produk cocok jika terhubung ke kategori itu melalui `product_categories` dalam tenant yang sama. Bila parameter dihilangkan, daftar mencakup semua kategori. UUID kategori yang tidak dikenal di toko tersebut, termasuk UUID milik toko lain, menghasilkan `200` dengan `data: []` dan `meta.totalElements: 0`. |

- Untuk `q=arabika`, **Biji Arabika 250 g** dan **Kopi Arabika Gayo** cocok. ` ARABIKA ` dan `arabik` juga cocok dengan keduanya. **Kopi Gayo** tersaring jika `arabika` hanya ada dalam deskripsinya. Pencarian memeriksa nama produk saja.
- **Kopi Arabika Gayo** dengan Primary Category **Kopi** dan kategori sekunder **Oleh-oleh Gayo** cocok dengan filter untuk salah satu kategori itu. Setiap produk yang cocok muncul sekali. Ikuti aturan kepemilikan dan relasi di [Product Management — PM-01 dan PM-02](product-management.md#tenant-kategori-dan-permission).
- Bila kedua parameter aktif, terapkan kedua kondisi pada daftar produk terbit milik tenant sebelum pengurutan dan pagination. Pakai kondisi yang sama untuk data hasil dan `meta.totalElements`. Untuk toko yang terpetakan, pencarian tanpa kecocokan menghasilkan `200` dengan `data: []` dan `meta.totalElements: 0`.
- Urutkan hasil dengan urutan daftar yang sudah berlaku, yaitu nama produk lalu ID untuk nama yang sama. **Biji Arabika 250 g** tampil sebelum **Kopi Arabika Gayo**. Pertahankan pagination yang berlaku dengan `page` mulai dari `0`, `size` bawaan `20`, dan ukuran maksimum `100`. Permintaan tanpa kriteria mengikuti perilaku daftar yang sudah ada.

### Tenant, publikasi, dan stok

- Server menentukan tenant dari hostname atau pasangan hostname dan prefiks toko. Cakupan tenant berlaku pada produk, kategori, relasi kategori, dan hitungan hasil. Jalur toko yang tidak terpetakan mengikuti respons `404` storefront. [Product Management — PM-08](product-management.md#publikasi-dan-storefront)
- Kandidat hasil adalah produk `PUBLISHED`. Produk `DRAFT` tersembunyi meski punya stok dan memenuhi kriteria. Produk terbit tetap dapat ditemukan saat semua variannya habis. Detail publik tetap membaca status per varian melalui Inventory dan menampilkan **Habis** untuk varian bersaldo nol. [Product Management — PM-07 dan PM-08](product-management.md#publikasi-dan-storefront) · [Inventory Management — IM-05](inventory-management.md#ketersediaan-untuk-storefront)

### Perilaku tahap frontend

- Sediakan pencarian nama dan satu pilihan kategori pada daftar publik `/products`. Pilihan kategori hanya mencakup kategori toko yang memiliki setidaknya satu produk terbit, termasuk relasi kategori sekunder dan produk terbit yang habis. Pilihan tetap tersedia saat pengunjung mengubah pencarian atau berpindah halaman. Kontrak penyediaan pilihan kategori ke halaman ditetapkan pada tahap frontend.
- Simpan `q` dan `categoryId` yang aktif di URL relatif terhadap toko. Pertahankan kriteria saat pengunjung memilih **Sebelumnya** atau **Berikutnya**, membuka ulang URL, atau memuat ulang halaman. Kembali ke `page=0` saat kata pencarian atau kategori berubah.
- Saat kriteria aktif menghasilkan total `0`, tampilkan **Tidak ada produk yang cocok**. Pertahankan kata pencarian dan kategori yang dipilih, serta sediakan satu tindakan untuk menghapus keduanya dan kembali ke halaman pertama daftar publik toko. Daftar tanpa kriteria mengikuti tampilan kosong yang sudah berlaku.
- Gunakan aturan identitas publik toko yang berlaku saat menampilkan daftar, termasuk saat hasilnya kosong. [Store Settings](store-settings.md#isian-penyimpanan-dan-tampilan)

## Testing Decisions

Gunakan Bruno untuk penerimaan HTTP. Jalankan dari `bruno/` dengan `bru run --env Local`. Jalankan skenario browser di tahap frontend melalui browser bawaan Orca dengan Orca CLI. Pakai data uji yang sama dan jalankan skenario publik tanpa login.

Petakan Toko A lewat prefiks di hostname platform dan Toko B lewat hostname langsung. Keduanya memiliki kategori **Kopi** dan produk terbit **Kopi Arabika Gayo**, dengan UUID kategori, ID produk, dan harga berbeda. Di Toko A, kelompokkan **Kopi Arabika Gayo** ke **Kopi** sebagai Primary Category dan **Oleh-oleh Gayo** sebagai kategori sekunder. Jadikan seluruh variannya habis. Tambahkan **Biji Arabika 250 g** yang terbit di **Kopi**, **Teh Gayo** yang terbit di **Teh** dan **Kopi** dengan teks `arabika` hanya di deskripsi, serta **Arabika Toraja** yang masih Draft dan memiliki stok. Masukkan Draft ke **Kopi** dan **Khusus Draft**, dengan kategori terakhir berisi Draft saja.

| ID | Metode | Kondisi, aksi, dan hasil yang teramati |
| --- | --- | --- |
| **PD-AT-01** | Bruno | Cari `arabika`, ` ARABIKA `, dan `arabik` di Toko A. Periksa hasil dalam urutan **Biji Arabika 250 g** lalu **Kopi Arabika Gayo**, dengan total `2`. **Teh Gayo** dan **Arabika Toraja** tersaring. Kueri yang hanya berisi spasi menghasilkan data dan metadata yang sama seperti daftar tanpa `q`. |
| **PD-AT-02** | Bruno | Filter **Oleh-oleh Gayo** dengan dan tanpa `q=arabika`. Keduanya mengembalikan satu kartu **Kopi Arabika Gayo** dengan total `1`. Filter **Kopi** tanpa pencarian mengembalikan seluruh produk terbit di kategori itu pada data uji, termasuk **Teh Gayo**. |
| **PD-AT-03** | Bruno | Gabungkan `q=arabika` dan kategori **Kopi** di Toko A dengan `size=1`. Periksa **Biji Arabika 250 g** pada `page=0` dan **Kopi Arabika Gayo** pada `page=1`. Kedua halaman melaporkan `totalElements=2` dan `totalPages=2`. Ganti filter ke **Oleh-oleh Gayo** pada `page=0` dan periksa bahwa **Kopi Arabika Gayo** langsung muncul dengan total `1`. Dalam skenario terpisah, cari dua produk terbit bernama sama di satu toko dan periksa urutan ID keduanya. |
| **PD-AT-04** | Bruno | Cari `arabika` dan pakai UUID **Kopi** milik masing-masing toko pada URL tokonya. Cocokkan ID dan harga kartu dengan data toko tersebut. Pakai UUID **Kopi** milik B pada URL A dan bandingkan hasilnya dengan UUID kategori yang tidak dikenal. Keduanya memberi `200`, daftar kosong, dan total `0`. URL toko yang tidak terpetakan memberi `404`. |
| **PD-AT-05** | Bruno | Cari `arabika` dalam kategori **Kopi** di Toko A. Draft **Arabika Toraja** absen dan detail publiknya memberi `404`. Produk terbit **Kopi Arabika Gayo** tetap muncul, dan detail publiknya mengembalikan `OUT_OF_STOCK` untuk semua variannya. |
| **PD-AT-06** | Bruno | Cari nama tanpa kecocokan dan gabungkan `arabika` dengan kategori **Teh** di Toko A. Periksa `200`, daftar kosong, dan total `0` pada kedua permintaan. |
| **PD-AT-07** | Browser | Buka URL kedua toko dan cari `arabika` dengan filter **Kopi**. Periksa kartu menggunakan harga yang berbeda. Pilihan kategori Toko A berisi **Kopi**, **Oleh-oleh Gayo**, dan **Teh**. Pilihan kategori Toko B berisi **Kopi**. Di Toko A, pilih **Oleh-oleh Gayo** untuk menemukan produk terbit yang habis. **Khusus Draft** dan kartu **Arabika Toraja** absen. Buka detail **Kopi Arabika Gayo** di Toko A dan periksa label **Habis** pada varian terdaftar. |
| **PD-AT-08** | Browser | Di Toko A, cari `arabika` dan pilih **Kopi** lewat kontrol halaman. Buka URL hasil yang sama dengan `size=1`. Klik **Berikutnya**, muat ulang, lalu klik **Sebelumnya**. Periksa urutan kartu, kriteria di URL, dan nilai kontrol. Dari halaman berikutnya, ubah kategori ke **Oleh-oleh Gayo** dan periksa hasil pada halaman pertama. Ulangi dari halaman berikutnya dengan mengubah kata pencarian untuk membuktikan reset halaman pada kedua kontrol. |
| **PD-AT-09** | Browser | Pilih **Teh** sambil mencari `arabika`. Periksa pesan **Tidak ada produk yang cocok**, kata pencarian, dan pilihan kategori. Gunakan tindakan hapus pencarian dan filter, lalu periksa bahwa daftar awal toko kembali pada halaman pertama. |

## Out of Scope

- Implementasi halaman dan kontrol frontend pada tahap API versi pertama.
- Pencarian lintas toko, pencarian deskripsi atau SKU, koreksi salah ketik, pengurutan berdasarkan relevansi, dan pilihan pengurutan baru.
- Filter beberapa kategori sekaligus, filter stok atau harga, serta halaman koleksi.
- Perubahan aturan pemetaan tenant, publikasi, stok, dan detail produk.

## Further Notes

Spec ini memperluas daftar publik yang tersedia melalui [Product 03 — Publikasi dan storefront tenant](../tickets/product-management/03-publikasi-dan-storefront.md). Bukti isolasi dan visibilitas katalog mengikuti IG-01 dan IG-02. [Module Roadmap](../planning/module-roadmap.md#module-matrix) · [Development Dependencies](../planning/development-dependencies.md#integration-gates--acceptance-scenarios)
