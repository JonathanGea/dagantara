# Store Settings — Identitas Publik Toko

## Problem Statement

Pengunjung daftar dan detail produk perlu tahu toko mana yang sedang mereka buka dan bagaimana menghubunginya. Admin tenant perlu bisa memperbarui nama tampilan, logo, dan kontak publik toko dari satu halaman pengaturan.

## Solution

Admin mengatur identitas publik melalui halaman **Store Settings** pada URL tokonya. Setelah penyimpanan berhasil, pengunjung melihat identitas terbaru pada daftar dan detail produk toko tersebut. Tenant yang belum menyiapkan pengaturan langsung memakai nama toko dari provisioning.

## User Stories

1. **SS-01** — Sebagai Admin Tenant yang berizin, saya bisa membuka dan menyimpan identitas publik toko saya agar pengunjung mengenali merek toko yang sedang dibuka.
2. **SS-02** — Sebagai Admin Tenant yang berizin, saya bisa mengunggah, mengganti, dan menghapus logo dalam form yang sama agar identitas toko bisa mengikuti perubahan merek.
3. **SS-03** — Sebagai pengunjung, saya bisa mengenali toko dari header dan menemukan kontak yang disediakan di footer, baik saat melihat daftar maupun detail produk.
4. **SS-04** — Sebagai Admin Tenant, saya bisa menyimpan perubahan dengan satu tindakan dan mendapat pesan jika penyimpanan gagal, sehingga saya bisa memperbaiki input sementara identitas publik sebelumnya tetap tampil.
5. **SS-05** — Sebagai pemilik toko, saya ingin akses pengaturan dan pembacaan logo mengikuti tenant yang terpetakan agar toko lain tidak bisa mengubah pengaturan saya atau menampilkan logo saya pada URL mereka.

## Implementation Decisions

### Akses dan kepemilikan

- Halaman **Store Settings** berada di `/admin/store-settings` relatif terhadap URL toko dan menggunakan alur login yang sudah ada. Gunakan konteks dari akun terautentikasi dan pastikan pemetaan URL cocok dengan tenant akun. Muat pengaturan setelah pemeriksaan akses berhasil dan tampilkan nama provisioning sebagai petunjuk untuk nama tampilan yang kosong. [User Management](user-management.md#login-dan-konteks-tenant)
- Gunakan permission `store_settings.manage` untuk membaca form dan menyimpan perubahan, termasuk mengganti dan menghapus logo. Berikan permission ke role bawaan `TENANT_ADMIN` pada tenant lama dan saat provisioning tenant baru. Role tenant lain dapat menerima izin melalui aturan delegasi yang berlaku. Akun platform tidak dapat mengelola Store Settings. [Role dan permission](user-management.md#role-permission-dan-akun-yang-dilindungi)
- Simpan paling banyak satu record Store Settings per tenant dengan `tenant_id` sebagai primary key dan foreign key ke `tenants.id`. Nama tampilan, kontak, dan referensi logo bersifat opsional. Ketiadaan record setara dengan semua isian kosong. Gunakan UUID tenant yang ditentukan server untuk pembacaan dan penyimpanan. Tetap gunakan `tenants.name` sebagai sumber nama provisioning.

### Isian, penyimpanan, dan tampilan

| Isian | Perilaku form | Perilaku di storefront |
| --- | --- | --- |
| Nama tampilan | Teks opsional hingga 255 karakter. Buang spasi di awal dan akhir. | Tampilkan di header. Jika kosong, gunakan `tenants.name`. Misalnya, `Kopi Pagi Nusantara` bisa tampil sebagai `Kopi Pagi`. |
| Logo | Tunjukkan logo saat ini jika ada. Admin bisa memilih file baru, mempertahankan logo, atau memilih **Hapus logo**. | Tampilkan di samping nama. Jika logo kosong atau gagal dimuat, tampilkan nama toko saja. |
| WhatsApp | Nomor publik opsional dalam format internasional dengan `+` dan kode negara. | Tampilkan tautan **Hubungi via WhatsApp** yang dibentuk dari digit nomor tersebut. |
| Email | Alamat publik opsional yang harus berupa satu alamat email valid. | Tampilkan tautan email di footer. |

- Tampilkan header dan footer yang sama pada `/products` dan `/products/{productId}` relatif terhadap URL toko, termasuk daftar tanpa produk. Footer menampilkan kanal kontak yang terisi. Jika kedua kanal kosong, sembunyikan bagian kontak. Kontak harus diisi secara eksplisit. Nomor dari pengajuan, provisioning, atau pemulihan akun tetap digunakan untuk alur internalnya. [Pengajuan dan provisioning](user-management.md#pengajuan-dan-provisioning-tenant)
- Buang spasi di awal dan akhir kontak sebelum validasi. Simpan teks kosong sebagai nilai kosong. Tampilkan teks sebagai teks biasa dan bentuk tautan dari nilai kontak yang telah divalidasi.
- Satu tindakan **Simpan** menerapkan semua isian dan aksi logo. Setelah berhasil, form menunjukkan nilai tersimpan dan kunjungan publik baru melihat nilai terbaru. Jika validasi atau penyimpanan gagal, tampilkan pesan yang sesuai dan pertahankan seluruh pengaturan sebelumnya.
- Gunakan pemetaan hostname dan prefiks toko yang sudah dipakai [Product Management](product-management.md#publikasi-dan-storefront) untuk menentukan tenant pada halaman dan permintaan logo publik. Permintaan katalog tetap mengikuti kontrak daftar, detail, dan publikasi yang ada. Tampilan publik hanya membutuhkan nama efektif, logo aktif, dan kontak yang dipilih admin.

### Siklus logo dan penyimpanan lokal

- Terima satu gambar PNG, JPEG, atau WebP, dengan ukuran maksimum 2 MiB (2.097.152 byte). Periksa format dan keterbacaan isi gambar. Nama file dan tipe yang dikirim klien tidak menentukan tipe gambar yang disajikan.
- Simpan gambar dalam direktori yang dapat dikonfigurasi pada mesin backend lokal. Buat nama penyimpanan dari identitas yang dibuat server. Simpan referensi dan tipe logo aktif di record pengaturan tenant. Logo tersedia setelah restart jika database dan direktori yang sama masih tersedia.
- Siapkan file baru sebelum menerbitkan referensinya. Publikasikan referensi dan semua isian dalam satu transaksi database. Jika penulisan file atau transaksi gagal, pengunjung tetap mendapat logo dan isian lama. Setelah penggantian atau penghapusan sukses, hanya logo yang aktif dapat diakses, termasuk saat penyimpanan dilakukan bersamaan. Bersihkan file yang sudah tidak dirujuk tanpa menghapus logo aktif.
- Sajikan gambar lewat rute yang relatif terhadap URL toko. Cari logo aktif dengan tenant hasil pemetaan dan ID logo. Gunakan tipe konten hasil validasi. URL dengan ID logo yang sudah diganti atau dihapus, logo tenant lain, logo yang filenya hilang, atau URL toko yang tidak terpetakan mendapat `404`. Atur cache halaman dan logo agar kunjungan baru melihat hasil simpan terbaru.

### Kontrak HTTP

Semua alamat di bawah relatif terhadap URL toko. Rute admin memakai autentikasi dan pemeriksaan tenant yang sudah ada. Respons JSON yang sukses memakai `ApiResponse`, dan respons error memakai `ProblemDetail`.

| Operasi | Akses dan hasil |
| --- | --- |
| `GET /api/v1/store-settings` | `store_settings.manage`. `200` dengan `tenantName`, `displayName`, `publicWhatsapp`, `publicEmail`, dan `logoUrl`. Isian yang belum tersimpan bernilai `null`. Jika ada logo, `logoUrl` berbentuk jalur dari root hostname dan menyertakan prefiks toko bila berlaku. |
| `PUT /api/v1/store-settings` | `store_settings.manage`. Ganti nilai form melalui `multipart/form-data`. Bagian JSON `settings` memuat `displayName`, `publicWhatsapp`, `publicEmail`, dan `logoAction`. `200` dengan pengaturan yang tersimpan dalam bentuk yang sama dengan hasil `GET`. |
| `GET /api/v1/storefront/logo/{logoId}` | Publik. `200` dengan gambar aktif milik tenant yang dipetakan dari URL, atau `404` jika tidak tersedia. |

Untuk menyimpan logo, `logoAction` bernilai `KEEP`, `REPLACE`, atau `REMOVE`. Bagian file `logo` wajib untuk `REPLACE` dan hanya boleh dikirim untuk aksi tersebut. Isian teks yang dikosongkan dikirim sebagai `null` atau string kosong. Ambil tenant dari konteks permintaan. API admin mengembalikan `401` jika autentikasi tidak valid, `403` jika izin atau cakupan tenant tidak cocok, `400` jika isian atau gambar tidak valid, dan `413` jika ukuran logo melampaui batas. Untuk kegagalan penyimpanan, kembalikan respons error tanpa membocorkan lokasi file.

## Testing Decisions

Jalankan skenario HTTP dengan Bruno dari `bruno/` memakai `bru run --env Local`. Gunakan database uji, direktori logo terpisah, dan fixture dua tenant. Petakan satu tenant melalui hostname langsung dan satu melalui prefiks di hostname Dagantara. Gunakan browser untuk memeriksa form, tampilan daftar dan detail, dan hasil setelah restart lokal. Untuk bukti kegagalan penyimpanan yang sulit dipicu lewat API, gunakan tes integrasi dengan direktori uji.

| ID | Cerita | Kondisi, aksi, dan hasil yang teramati |
| --- | --- | --- |
| **SS-AT-01** | SS-01, SS-03 | Login sebagai Admin Tenant lama yang belum punya pengaturan dan buka form. API mengembalikan isian kosong dan nama provisioning. Cek nama itu pada header daftar kosong, lalu pada daftar dan detail setelah tersedia produk terbit. Bagian kontak tidak terlihat. |
| **SS-AT-02** | SS-01, SS-03 | Simpan nama `Kopi Pagi`, WhatsApp, dan email pada A. Baca ulang pengaturan A, lalu buka daftar dan detailnya. Kedua halaman menunjukkan nama baru dan tautan ke nilai kontak A. Kosongkan nama dan email, lalu simpan. Kedua halaman memakai nama provisioning dan hanya menunjukkan tautan WhatsApp. Kosongkan juga WhatsApp dan periksa bahwa bagian kontak hilang. |
| **SS-AT-03** | SS-02, SS-03 | Unggah logo yang sah lalu periksa gambar pada daftar dan detail. Simpan perubahan teks dengan `KEEP` dan pastikan URL logo yang sama masih bekerja. Restart backend dengan direktori yang sama, lalu pastikan gambar tetap tersedia. Ganti dan hapus logo secara berurutan. Setiap kali baca ulang pengaturan, periksa tampilan kedua halaman, dan pastikan URL logo terdahulu mengembalikan `404`. Saat file aktif tidak tersedia, nama toko tetap tampil. |
| **SS-AT-04** | SS-02, SS-04 | Dengan pengaturan tersimpan, kirim nama baru bersama file yang bukan gambar, lalu ulangi dengan gambar yang melampaui batas. Cek masing-masing `400` dan `413`. Uji juga kontak tidak valid dan kegagalan menulis file baru. Pembacaan admin dan storefront tetap menunjukkan seluruh nilai sebelumnya, termasuk logo aktif. |
| **SS-AT-05** | SS-01, SS-05 | Coba baca dan simpan pengaturan tanpa sesi, dengan akun tenant tanpa izin, dan dengan akun platform. Periksa `401` atau `403` sesuai kontrak. Gunakan sesi admin A dengan payload valid pada URL B dan periksa `403`. Baca kembali pengaturan A dan B sebagai admin masing-masing dan periksa bahwa percobaan itu tidak mengubah keduanya. |
| **SS-AT-06** | SS-03, SS-05 | Beri A dan B nama, logo, kontak, dan produk terbit yang berbeda. Simpan perubahan hanya pada A dan buka daftar serta detail kedua toko. Setiap URL hanya menunjukkan identitas dan produk tenant hasil pemetaan. Akses ID logo A melalui URL B dan melalui URL yang tidak terdaftar, lalu periksa `404` pada keduanya. |

## Out of Scope

- Penyimpanan logo untuk deployment dan layanan penyimpanan eksternal.
- Pengaturan tema, editor layout, menu navigasi, dan halaman kontak terpisah.
- Perubahan nama provisioning, domain, prefiks toko, dan siklus hidup tenant.
- Unggah gambar untuk media produk serta perubahan kontrak HTTP produk yang sudah dipublikasikan.

## Further Notes

- Store Settings menentukan identitas yang dipakai storefront dasar. Pemetaan URL toko tetap mengikuti aturan User Management dan Product Management. Isolasi diuji melalui gate **IG-01**. Siklus hidup tenant dan domain tetap berada pada **DEP-01**. [Roadmap](module-roadmap.md#module-matrix) · [Development Dependencies](development-dependencies.md#integration-gates--acceptance-scenarios)
- Penyimpanan file lokal dalam spesifikasi ini dikhususkan untuk logo Store Settings. [Backend Foundation](../backend/README.md#scope)

## Implementation Tickets

Menunggu persetujuan pembagian tiket.

| No. | Hasil | Prasyarat |
| --- | --- | --- |
| 01 | Admin tenant menyimpan nama tampilan dan kontak publik. Pengunjung melihat nama dan kontak yang tepat pada daftar dan detail. Bruno dan browser membuktikan fallback, permission, dan isolasi tenant. | Dapat dimulai dengan pemetaan URL, login, dan storefront yang tersedia. |
| 02 | Admin mengunggah, mengganti, dan menghapus logo lokal dalam tindakan Simpan yang sama. Bruno dan browser membuktikan tampilan, persistensi setelah restart, penolakan file, dan isolasi URL logo. | 01 menyediakan form, izin, pengaturan per tenant, dan header storefront. |
