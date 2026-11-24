# Product Management

## Problem Statement

Pengelola toko perlu mencatat satu produk dengan beberapa kombinasi pilihan, seperti Kaos Basic warna Hitam ukuran M dan warna Putih ukuran L. Petugas penerimaan perlu mengenali kombinasi yang tepat melalui SKU. Pengunjung perlu melihat produk yang sudah terbit, foto yang sesuai dengan pilihan, serta ketersediaan kombinasi yang dipilih.

## Solution

Setiap tenant mengelola katalog, kategori, media, opsi, dan SKU miliknya. Pengelola menyiapkan produk sebagai Draft, lalu menerbitkannya saat siap. Storefront membaca katalog toko yang sedang dibuka dan mengambil status ketersediaan SKU dari Inventory Management.

## Admin UI Structure

Referensi Tenant Admin menempatkan Products, Categories, Brands, Collections, dan Option Library di bawah Catalog. Products dan Categories menyediakan konteks navigasi untuk pengelolaan lewat API di spec ini.

## User Stories

1. Sebagai pengelola katalog, saya bisa membuat kategori dan produk untuk toko saya serta memilih satu kategori utama agar produk mudah dikelompokkan.
2. Sebagai pengelola katalog, saya bisa membuat opsi dan menetapkan SKU otomatis atau manual untuk setiap kombinasi yang disediakan toko agar petugas dapat mencatat barang yang tepat.
3. Sebagai pengelola katalog, saya bisa menyimpan URL foto dan menautkan foto ke nilai opsi agar pengunjung melihat media yang sesuai pilihannya.
4. Sebagai pengelola katalog, saya bisa melengkapi harga, menerbitkan produk, menariknya ke Draft, dan menerbitkannya kembali agar saya dapat mengatur kapan produk terlihat.
5. Sebagai pengunjung, saya bisa melihat daftar dan detail produk terbit di toko yang saya buka, memilih kombinasi, dan mengetahui ketersediaannya.

## Functional Specification

| ID | Perilaku |
| --- | --- |
| **PM-01** | Setiap tenant memiliki katalog independen. Produk dan kategori dimiliki tenant. |
| **PM-02** | Produk terhubung ke satu atau lebih kategori milik tenant yang sama dan memiliki tepat satu Primary Category dari daftar tersebut sejak dibuat. |
| **PM-03** | Media URL dimiliki satu produk. |
| **PM-04** | Media dan nilai opsi pada produk yang sama dapat ditautkan many-to-many. |
| **PM-05** | Produk dengan opsi memiliki satu varian per kombinasi yang didaftarkan. Produk tanpa opsi memiliki satu varian bawaan. |
| **PM-06** | Setiap varian memiliki satu SKU unik per tenant yang bisa dihasilkan otomatis atau diisi manual. Kode bisa diperbaiki sebelum transaksi stok pertama. |
| **PM-07** | Produk baru berstatus Draft. Pengelola dapat menerbitkan produk yang sudah punya harga rupiah dan varian, atau menarik produk terbit kembali ke Draft. |
| **PM-08** | Storefront menampilkan daftar dan detail produk terbit dalam tenant yang dipetakan dari URL toko, termasuk galeri dan status stok varian. |

## Implementation Decisions

### Tenant, kategori, dan permission

- Gunakan tenant akun terautentikasi sesuai [User Management](user-management.md). Jika URL request juga memetakan tenant, pemetaan harus cocok dengan akun. Akses data katalog admin memerlukan tenant aktif dan permission dari role aktif.
- `catalog.read` mengizinkan daftar dan detail admin. `catalog.manage` mengizinkan pembuatan kategori serta perubahan produk, opsi, SKU, media, dan publikasi. Kedua permission tersedia di katalog permission untuk role tenant. Lookup objek dan pagination dilakukan dalam tenant akun.
- Daftar kategori produk adalah himpunan kategori milik tenant produk. Saat membuat produk atau mengganti daftar, kirim `categoryIds` yang berisi setidaknya satu kategori serta `primaryCategoryId` yang merupakan salah satu anggotanya. Simpan keduanya dalam satu transaksi.
- Produk punya UUID, nama, deskripsi opsional, dan satu harga untuk seluruh variannya. Harga memakai bilangan bulat rupiah (`priceIdr`), boleh nol, dan boleh kosong selama Draft. Foto dan deskripsi bersifat opsional saat terbit. Produk yang terbit harus tetap memenuhi syarat harga dan varian saat diperbarui.

### Opsi, varian, dan SKU

- Opsi memiliki nama dan urutan dalam produk. Nilai opsi memiliki identitas sendiri di bawah opsi tersebut. Contoh opsi `Warna` memiliki nilai `Hitam` dan `Putih`.
- Satu varian untuk produk beropsi berisi tepat satu nilai dari tiap opsi produk. Admin mendaftarkan kombinasi yang disediakan toko. Kombinasi yang sama hanya bisa didaftarkan sekali. Produk tanpa opsi memperoleh satu varian bawaan saat dibuat.
- Varian memiliki UUID stabil yang dipakai Inventory sebagai referensi. Kode SKU mengidentifikasi varian dalam tenant dan pencariannya mengabaikan kapitalisasi. Nilai pilihan yang membentuk satu varian adalah bagian dari identitas varian tersebut. Kombinasi lain mendapatkan varian baru.
- Saat membuat varian, admin memilih mode `AUTO` atau `MANUAL`. Generator memakai satu pola bawaan. Gabungkan kode singkat produk dan kode nilai pilihan sesuai urutan opsi dengan pemisah tanda hubung. Sebagai contoh, kode produk `KBS` dan nilai `HTM` serta `M` menghasilkan `KBS-HTM-M`. Varian bawaan memakai kode produk saja. Kode singkat berupa alfanumerik yang disediakan admin untuk data yang menggunakan generator. Mode manual menerima SKU yang ditentukan admin.
- Klien admin dapat memakai pola yang sama untuk pratinjau. Server menghasilkan SKU final saat menyimpan. Normalisasikan kode ke huruf besar setelah memangkas spasi tepi. SKU manual boleh memuat huruf ASCII, angka, titik, garis bawah, dan tanda hubung. Konflik SKU dalam tenant atau kombinasi yang sudah ada memberi `409`. Admin dapat memperbaiki input atau memilih mode manual.
- SKU boleh diperbaiki sebelum transaksi stok pertama. Setelah ada catatan barang masuk, kode dan kombinasi SKU terkunci, termasuk ketika saldo kemudian kembali nol. Pengecekan pada perubahan SKU dan pencatatan stok harus diserialisasi untuk varian yang sama. Perubahan label mempertahankan identitas nilai opsi. Kode singkat yang diperbarui berlaku untuk hasil generate berikutnya dan tidak menghitung ulang SKU tersimpan.
- Daftar dimensi dan urutan opsi ditetapkan saat produk dibuat. Admin dapat menambah nilai pada opsi yang sudah didefinisikan dan mendaftarkan kombinasi baru dari nilai-nilai tersebut.

### Media produk

- URL media disimpan pada produk. Urutkan media menurut waktu penambahan dengan ID sebagai pembeda saat waktunya sama. Foto pertama menjadi foto kartu produk.
- Penggantian `optionValueIds` berlaku untuk satu media dalam satu transaksi. Daftar kosong melepas semua tautan pada media tersebut. Setiap nilai opsi yang dikirim harus berasal dari produk pemilik media. Satu nilai opsi tetap bisa ditautkan ke beberapa media.
- Payload admin detail produk mengembalikan kategori dan Primary Category, opsi dan nilainya, varian beserta SKU, serta media dan daftar `optionValueIds` per media.

### Publikasi dan storefront

- Status tersimpan adalah `DRAFT` dan `PUBLISHED`. Buat produk sebagai `DRAFT`. Penerbitan memerlukan harga yang sah dan setidaknya satu varian. Menarik produk mengubahnya kembali ke `DRAFT`. Produk Draft juga dapat menerima stok. Pengiriman ulang status yang sudah aktif bersifat idempoten.
- Pemetaan domain aktif atau pasangan hostname dan jalur toko menentukan tenant publik. Gunakan pemetaan yang diverifikasi server. Identitas tenant dari parameter klien tidak memberi akses ke katalog lain. Tenant atau target publik yang tidak tersedia, produk Draft, dan produk milik tenant lain menghasilkan `404` yang setara.
- Daftar publik menampilkan kartu dengan nama, harga rupiah, dan foto pertama bila tersedia. Detail menampilkan nama, harga, deskripsi bila tersedia, kategori, galeri, serta opsi dan kombinasi varian yang terdaftar. ID produk dan varian pada respons publik cukup untuk resolusi dan tampilan.
- Galeri awal menampilkan media produk dalam urutan tetap. Saat ada nilai terpilih yang punya media, tampilkan media untuk nilai terpilih pertama menurut urutan opsi. Tampilkan galeri awal bila tidak ada nilai terpilih yang memiliki foto. Pada contoh Warna lalu Ukuran, memilih Hitam menampilkan foto Hitam, dan pemilihan ukuran tidak menggantinya jika foto Hitam tersedia.
- Untuk produk beropsi, evaluasi stok saat pilihan membentuk satu varian terdaftar. Pilihan yang belum lengkap meminta pengunjung melengkapinya. Kombinasi yang tidak terdaftar ditandai sebagai kombinasi yang tidak disediakan toko. Produk tanpa opsi langsung menggunakan varian bawaan.
- Baca status dari [Inventory Management](inventory-management.md#ketersediaan-untuk-storefront) dengan tenant dan UUID varian. Saldo positif ditampilkan sebagai **Tersedia**, saldo nol sebagai **Habis**. Produk terbit dapat dilihat meski semua variannya habis. Informasi publik ketersediaan berupa status. Sumber saldo dan agregat angka tersedia melalui API admin Inventory.

### Kontrak HTTP

Gunakan respons sukses `ApiResponse`, pagination repo untuk daftar, dan `ProblemDetail` untuk error. Permission dinilai sebelum lookup. Request tanpa autentikasi pada jalur admin memberi `401`, akun tanpa tenant atau tanpa permission memberi `403`, dan target yang tidak ada dalam tenant memberi `404`. Data atau referensi body yang tidak valid memberi `400`, termasuk kategori atau nilai opsi di luar himpunan yang sah, dengan pesan yang tidak mengungkap entitas tenant lain. Perubahan dengan konflik keadaan, seperti SKU yang sudah digunakan atau penerbitan Draft yang belum memenuhi syarat, memberi `409`.

| Metode dan path relatif | Izin | Input dan hasil sukses |
| --- | --- | --- |
| `POST /api/v1/catalog/categories` | `catalog.manage` | `name`. `201` dengan ID dan nama kategori. |
| `GET /api/v1/catalog/categories` | `catalog.read` | Daftar kategori dalam tenant. `200` dengan pagination. |
| `POST /api/v1/catalog/products` | `catalog.manage` | `name`, `description`, `priceIdr`, `categoryIds`, `primaryCategoryId`, `productCode` untuk mode otomatis, serta `options` berisi nama dan daftar awal nilai dengan label dan kode opsional. Urutan `options` menentukan pola SKU. Sertakan `skuMode` dan `sku` bila manual untuk produk tanpa opsi. `201` dengan detail Draft. |
| `GET /api/v1/catalog/products` | `catalog.read` | Daftar ID, nama, harga, status, dan ID kategori utama. `200` dengan pagination. |
| `GET /api/v1/catalog/products/{productId}` | `catalog.read` | `200` dengan detail produk admin. |
| `PUT /api/v1/catalog/products/{productId}/details` | `catalog.manage` | Ganti `name`, `description`, `priceIdr`, dan `productCode`. `200` dengan detail terbaru. |
| `PUT /api/v1/catalog/products/{productId}/categories` | `catalog.manage` | Daftar akhir `categoryIds` dan `primaryCategoryId`. `200` dengan kategori produk yang tersimpan. |
| `POST /api/v1/catalog/products/{productId}/options/{optionId}/values` | `catalog.manage` | Tambah label dan kode opsional untuk generator. `201` dengan nilai opsi. |
| `PUT /api/v1/catalog/products/{productId}/options/{optionId}/values/{valueId}/details` | `catalog.manage` | Perbarui label nilai opsi dan kode singkat untuk hasil generate berikutnya. `200` dengan nilai opsi pada ID yang sama. |
| `POST /api/v1/catalog/products/{productId}/variants` | `catalog.manage` | `optionValueIds`, `skuMode`, serta `sku` bila manual. `201` dengan ID varian, kombinasi, dan SKU. |
| `PUT /api/v1/catalog/products/{productId}/variants/{variantId}/sku` | `catalog.manage` | `skuMode` dan `sku` bila manual. `200` dengan SKU terbaru jika belum ada transaksi. |
| `POST /api/v1/catalog/products/{productId}/media` | `catalog.manage` | URL absolut HTTP atau HTTPS. `201` dengan ID media, URL, dan daftar tautan kosong. |
| `PUT /api/v1/catalog/products/{productId}/media/{mediaId}/option-values` | `catalog.manage` | Daftar akhir `optionValueIds`. `200` dengan media dan tautan terbaru. |
| `PUT /api/v1/catalog/products/{productId}/publication` | `catalog.manage` | `status` berupa `DRAFT` atau `PUBLISHED`. `200` dengan status tersimpan. |
| `GET /api/v1/storefront/products` | Publik pada URL toko | `200` dengan kartu produk terbit dan pagination. |
| `GET /api/v1/storefront/products/{productId}` | Publik pada URL toko | `200` dengan detail produk terbit, termasuk status ketersediaan per varian. |

Alamat halaman publik adalah `/products` dan `/products/{productId}` relatif terhadap URL toko. Jalur API publik juga mengikuti URL toko, termasuk prefiks jalur bila toko dipetakan melalui prefiks.

## Data Model

| Data | Kepemilikan dan integritas |
| --- | --- |
| `products`, `categories` | UUID dan `tenant_id` wajib. Satu tenant dapat membuat kategorinya sendiri dengan nama yang juga dipakai tenant lain. |
| `product_categories` | Pasangan `(product_id, category_id)` unik. FK gabungan dengan tenant memastikan produk dan kategori berada dalam tenant yang sama. |
| Primary Category | Simpan pada `products.primary_category_id` dengan `NOT NULL`. FK gabungan `(id, primary_category_id)` ke `product_categories(product_id, category_id)` diperiksa pada commit sehingga pembuatan produk dan penggantian kategori dapat disimpan sebagai satu transaksi. |
| `product_options`, `product_option_values` | Opsi dan nilai dimiliki produk yang sama. Identitas nilai mengikat tautan media dan varian. |
| `product_variants` dan relasi nilai opsi | UUID varian stabil. Terapkan keunikan SKU per tenant dan kombinasi nilai per produk. Relasi ke nilai opsi harus tetap dalam produk yang sama. |
| `product_media`, `media_option_values` | Media dimiliki produk. Pasangan `(media_id, option_value_id)` unik dan kedua sisi harus milik produk yang sama. |

## Testing Decisions

Jalankan skenario HTTP dengan Bruno melalui `bru run --env Local` dari `bruno/`. Gunakan dua tenant dengan role berizin dan akun baca-saja. Pakai PostgreSQL untuk menguji constraint kategori dan keunikan SKU. Uji halaman publik melalui browser menggunakan API dan data stok yang sama.

## Acceptance Scenarios

| ID | Requirement | Kondisi, aksi, dan hasil |
| --- | --- | --- |
| **PM-AT-01** | PM-01, PM-04 | Akun B dengan izin kelola mencoba menautkan media produk A. Dapat `404` dan pembacaan ulang oleh akun A menunjukkan tautan awal. Daftar admin B hanya berisi produk B. |
| **PM-AT-02** | PM-02 | Buat produk dengan dua kategori A dan pilih salah satunya. Ganti daftar serta pilih Primary Category baru. Detail menunjukkan tepat satu kategori utama dari daftar terbaru. Kategori B atau primary di luar daftar memberi `400` tanpa perubahan parsial. |
| **PM-AT-03** | PM-03, PM-04 | Tautkan satu media ke dua nilai opsi produk. Detail mengembalikan kedua nilai dan media tetap dimiliki produk tersebut. Nilai dari produk lain memberi `400` dan tautan sebelumnya tetap berlaku. |
| **PM-AT-04** | PM-03, PM-04 | Tautkan dua media ke satu nilai opsi. Detail mengembalikan kedua relasi. Simpan daftar kosong pada satu media dan periksa bahwa hanya tautan media tersebut yang hilang. |
| **PM-AT-05** | PM-05, PM-06 | Buat Hitam/M otomatis dan Putih/L manual. Periksa kode dan kombinasi. Duplikasi kombinasi atau SKU, termasuk beda kapitalisasi, memberi `409`. Produk tanpa opsi memiliki satu varian bawaan. |
| **PM-AT-06** | PM-06 | Ganti SKU sebelum penerimaan pertama. Terima stok dengan SKU baru, lalu coba ubah lagi. Perubahan ditolak dengan `409` dan lookup stok tetap merujuk varian yang sama. |
| **PM-AT-07** | PM-07 | Produk baru tidak muncul secara publik. Coba terbitkan tanpa harga dan dapat `409`. Isi harga lalu terbitkan. Tarik dan terbitkan lagi, serta cek daftar dan URL detail publik pada setiap transisi. |
| **PM-AT-08** | PM-08 | Buka daftar dan detail produk terbit. Cek harga, galeri yang mengikuti pilihan, status SKU bersaldo positif atau nol, serta hasil saat kombinasi belum lengkap atau tidak terdaftar. Saat seluruh SKU habis, detail produk tetap dapat dibuka. ID produk A dari URL tenant B memberi `404`. |
| **PM-AT-09** | PM-01, PM-07 | Akun baca-saja dapat melihat detail namun percobaan perubahan memberi `403`. Request admin tanpa login memberi `401`. Produk dan statusnya tetap sama setelah penolakan. |

## Out of Scope

- Pembuatan UI admin, unggah file, Brands, Collections, Option Library, dan pengaturan tema storefront.
- Harga per varian, promosi, keranjang, checkout, dan pemesanan. Pencarian dan filter katalog diatur di [Product Discovery](product-discovery.md).
- Pengelolaan stok, penerimaan, koreksi, dan multi-gudang. Pengelolaan stok versi pertama ditetapkan di [Inventory Management](inventory-management.md).

## Further Notes

Integrasi admin membutuhkan identitas tenant, login, dan evaluasi permission sesuai User Management. Integrasi publik membutuhkan pemetaan URL toko aktif. Detail ketersediaan end-to-end membutuhkan saldo per varian dari Inventory Management. Rute pengelolaan katalog dan storefront diperkenalkan dalam spesifikasi ini.

## Implementation Tickets

- [Product 01 — Katalog Draft, kategori, opsi, dan SKU](../tickets/product-management/01-katalog-draft-dan-sku.md)
- [Product 02 — Media dan tautan nilai opsi](../tickets/product-management/02-media-dan-tautan-opsi.md)
- [Product 03 — Publikasi dan storefront tenant](../tickets/product-management/03-publikasi-dan-storefront.md)
