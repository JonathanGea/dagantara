# Tenant Management — Penonaktifan dan Aktivasi Kembali Tenant

## Problem Statement

Operator platform perlu menemukan toko yang sudah diprovisioning, menghentikan aksesnya, dan memulihkannya tanpa kehilangan alamat atau data toko. Pemilik dan staf perlu mengetahui kapan mereka dapat kembali bekerja. Pengunjung yang membuka tautan saat toko nonaktif perlu mendapat pesan yang jelas tanpa melihat katalog toko.

## Solution

Operator berizin melihat daftar tenant dan mengubah status tenant yang dipilih. Saat tenant nonaktif, storefront dan akses internal ditutup. Saat diaktifkan kembali, storefront tersedia di alamat yang sama, data toko tetap tersedia, dan akun internal yang memenuhi syarat dapat login ulang.

## User Stories

1. **TM-01** — Sebagai operator platform dengan izin baca, saya dapat melihat ID, nama, URL toko, dan status semua tenant yang sudah diprovisioning agar dapat membedakan toko bernama sama dan memilih target yang tepat.
2. **TM-02** — Sebagai operator platform dengan izin penonaktifan atau aktivasi kembali, saya dapat melakukan tindakan yang diizinkan pada tenant yang dipilih agar dapat menghentikan atau memulihkan aksesnya.
3. **TM-03** — Sebagai pengunjung, saya mendapat pesan “Toko tidak tersedia” saat membuka halaman katalog tenant nonaktif, dan saya dapat memakai kembali tautan yang sama setelah toko aktif.
4. **TM-04** — Sebagai pemilik atau staf tenant, akses internal saya berhenti saat tenant nonaktif. Setelah tenant aktif kembali, saya dapat login ulang jika akun saya masih memenuhi syarat.

## Implementation Decisions

### Status tenant dan pemetaan URL

- Simpan status pada `tenants` dengan nilai `ACTIVE` atau `INACTIVE`. Tenant yang sudah ada berstatus `ACTIVE` saat migrasi. Penyelesaian pengajuan dan onboarding langsung membuat tenant `ACTIVE` dalam transaksi provisioning yang sudah berlaku. [User Management](user-management.md#pengajuan-dan-provisioning-tenant)
- Pertahankan pemetaan unik `(hostname, shop_path)` pada `tenant_domains` untuk domain toko dan alamat Dagantara dengan jalur toko. Penonaktifan tetap mencadangkan alamat itu. Provisioning tenant lain dengan alamat yang sama menghasilkan `409 LOGIN_URL_IN_USE`.
- Perubahan status berlaku pada tenant yang ditargetkan saja. Pertahankan ID tenant, pemetaan URL, status akun dan role, serta data operasional seperti katalog dan stok. Aktivasi kembali membuka akses melalui pemetaan yang sama.

### URL publik tenant nonaktif

- Tentukan tenant dari pemetaan yang diperiksa server, lalu periksa status aktif sebelum membaca data publik. Pada tenant nonaktif, API storefront mengembalikan `404 NOT_FOUND` seperti alamat toko yang tidak terpetakan. Respons tidak memuat data toko.
- Halaman daftar `/products` dan detail `/products/{productId}`, relatif terhadap URL toko, mengembalikan HTML `404` dengan pesan umum “Toko tidak tersedia” bagi pengunjung tenant nonaktif. Setelah aktivasi kembali, halaman dan API mengikuti aturan publikasi yang berlaku. [Product Management — PM-08](product-management.md#publikasi-dan-storefront)

### Akses internal tenant nonaktif

- Tolak login pemilik, admin, dan staf tenant nonaktif dengan `401 INVALID_CREDENTIALS`. Permintaan ke rute internal yang dilindungi dengan sesi yang dicabut menghasilkan `401`.
- Terapkan perubahan status dan pencabutan sesi tenant dalam satu transaksi saat berpindah dari `ACTIVE` ke `INACTIVE`. Setelah transaksi berhasil, permintaan baru dengan sesi terdahulu ditolak. Pertahankan pemeriksaan status tenant di jalur login dan validasi sesi.
- Setelah aktivasi kembali, sesi yang telah dicabut tetap tidak berlaku. Akun yang masih memenuhi syarat bisa login ulang dengan kredensialnya, termasuk mengikuti kewajiban ganti password sementara jika berlaku. [User Management](user-management.md#login-dan-konteks-tenant)

### API pengelolaan tenant

- Gunakan akses platform dan permission eksplisit dari role efektif. Berikan ketiga permission pada role Admin Platform bawaan dan sediakan melalui mekanisme delegasi platform yang berlaku. Izin `tenants.provision` hanya mencakup provisioning. [User Management](user-management.md#role-permission-dan-akun-yang-dilindungi)
- Respons daftar dan tindakan memakai `ApiResponse` dengan data `id` (UUID), `name`, `storeAddress`, dan `status`. `storeAddress` menampilkan hostname dan jalur toko yang tersimpan, misalnya `toko.dagantara/kopi-gayo` atau `kopigayo.example`. Nilai ini berlaku juga untuk tenant lama yang hanya menyimpan hostname dan jalur.
- Daftar berisi tenant aktif dan nonaktif. Gunakan pagination standar API (`page` dan `size`) dengan urutan nama lalu ID agar hasil tetap deterministik untuk toko bernama sama.

| Operasi | Permission platform | Hasil sukses |
| --- | --- | --- |
| `GET /api/v1/tenants` | `tenants.read` | `200` dengan daftar tenant dan metadata pagination. |
| `POST /api/v1/tenants/{tenantId}/deactivate` | `tenants.deactivate` | `200` dengan tenant berstatus `INACTIVE`. |
| `POST /api/v1/tenants/{tenantId}/reactivate` | `tenants.activate` | `200` dengan tenant berstatus `ACTIVE`. |

- Kembalikan `401` untuk permintaan tanpa autentikasi yang valid, `403` untuk akun terautentikasi tanpa cakupan atau izin yang sesuai, dan `404` untuk ID tenant yang tidak ditemukan setelah pemeriksaan izin.
- Periksa izin tindakan pada setiap permintaan. Permintaan berizin untuk status yang sudah berlaku mengembalikan status itu tanpa melakukan perubahan tambahan. Serialisasikan perubahan status pada tenant yang sama agar status dan pencabutan sesi konsisten.

## Testing Decisions

Gunakan Bruno untuk pengujian HTTP dengan `bru run --env Local` dari `bruno/`. Gunakan browser untuk memeriksa halaman storefront tanpa login. Siapkan tenant dengan domain toko dan tenant dengan jalur Dagantara, masing-masing dengan produk terbit dan akun internal. Jalankan siklus nonaktif dan aktif kembali untuk kedua jenis alamat secara bergantian agar tenant lainnya menjadi pembanding yang tetap aktif.

| ID | Requirement | Metode | Kriteria penerimaan |
| --- | --- | --- | --- |
| **TM-AT-01** | TM-01 | Bruno | Selesaikan provisioning dari pengajuan dan onboarding langsung. Operator dengan `tenants.read` melihat kedua tenant berstatus `ACTIVE` beserta ID, nama, dan alamat masing-masing. Tenant dari data uji sebelum migrasi juga muncul sebagai `ACTIVE` dengan ID dan alamat yang sama. Dua toko bernama sama tetap dapat dibedakan berdasarkan ID dan alamat. Setelah tenant dinonaktifkan, daftar masih memuatnya dengan status `INACTIVE`. |
| **TM-AT-02** | TM-01, TM-02 | Bruno | Operator dengan izin yang sesuai dapat menonaktifkan dan mengaktifkan kembali tenant. Ulangi tiap tindakan dan pastikan status tetap konsisten. Uji permintaan anonim, akun tenant, operator dengan izin provisioning saja, serta operator yang hanya memiliki izin untuk tindakan sebaliknya. Pastikan daftar atau tindakan ditolak sesuai izin dan periksa kembali status setelah penolakan. Uji juga `404` untuk ID tenant yang tidak dikenal dengan operator berizin. |
| **TM-AT-03** | TM-02, TM-03 | Bruno | Setelah penonaktifan, API daftar dan detail produk memberi `404` tanpa data toko. Provisioning tenant lain dengan pemetaan yang sama memberi `409`, dan daftar tenant menunjukkan hanya tenant asal memiliki alamat tersebut. Tenant pembanding tetap `ACTIVE` dan katalog publiknya tersedia. Setelah aktivasi kembali, API di alamat asal mengembalikan daftar dan detail produk yang sama. |
| **TM-AT-04** | TM-02, TM-04 | Bruno | Login sebagai pemilik dan staf aktif sebelum penonaktifan. Setelah penonaktifan, coba login baru dan lakukan permintaan internal dengan sesi lama. Keduanya ditolak dengan `401`. Setelah aktivasi kembali, sesi lama tetap ditolak, akun aktif bisa login ulang, dan akun yang sudah nonaktif sebelumnya tetap ditolak. Baca kembali data katalog atau stok untuk memastikan data tenant tetap tersedia. |
| **TM-AT-05** | TM-03 | Browser | Buka daftar dan detail produk sebelum penonaktifan. Setelah penonaktifan, muat ulang daftar dan buka tautan langsung ke detail. Keduanya menampilkan “Toko tidak tersedia” tanpa katalog. Setelah aktivasi kembali, tautan yang sama menampilkan katalog dan detail produk. |

## Out of Scope

- Penggantian URL toko setelah provisioning ditunda menjadi enhancement. URL yang disepakati saat onboarding tetap digunakan saat tenant aktif, nonaktif, dan aktif kembali.
- Otomasi penyiapan domain, penghapusan tenant, pengarsipan tenant, dan UI operator platform.

## Further Notes

**Enhancement — Penggantian URL toko.** Cakup perpindahan yang dilakukan operator setelah alamat tujuan siap. Desain enhancement perlu menetapkan perpindahan pemetaan storefront dan login secara atomik, penanganan konflik alamat, serta perilaku URL lama.

Cakupan ini menyelesaikan keputusan status tenant dan akses storefront pada [DEP-01](../planning/development-dependencies.md#dep-01-tenant-lifecycle). Verifikasi isolasi domain mengikuti [IG-01](../planning/development-dependencies.md#integration-gates--acceptance-scenarios).
