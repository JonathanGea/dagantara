# Tenant Admin — Tema Light, Dark, dan Ikuti Sistem

## Problem Statement

Tenant Admin selalu tampil dengan palet terang. Pengguna membutuhkan pilihan tampilan untuk bekerja dengan palet gelap atau mengikuti tema perangkat.

## Solution

Pengguna beralih langsung antara **Light** dan **Dark** dengan sakelar di header Tenant Admin. Kunjungan pertama mengikuti tema perangkat. Pengguna dapat kembali mengikuti tema perangkat melalui **Ikuti Sistem** di menu akun. Pilihan langsung diterapkan dan diingat oleh browser.

## User Stories

1. **TA-MODE-01** — Sebagai pengguna Tenant Admin, saya bisa beralih antara **Light** dan **Dark** langsung dari header di desktop dan seluler.
2. **TA-MODE-02** — Sebagai pengguna yang memilih **Ikuti Sistem**, saya ingin tampilan mengikuti tema perangkat saat pertama dibuka dan saat tema perangkat berubah.
3. **TA-MODE-03** — Sebagai pengguna Tenant Admin, saya ingin pilihan tema tetap aktif setelah halaman dimuat ulang atau browser dibuka kembali.
4. **TA-MODE-04** — Sebagai pengguna Tenant Admin, saya ingin teks, label status produk, dan indikator fokus keyboard tetap terbaca pada kedua palet.

## Implementation Decisions

- Letakkan sakelar tema di sisi kanan header Tenant Admin pada desktop dan seluler. Ikon matahari tetap di luar jalur sebelah kiri dan ikon bulan di luar jalur sebelah kanan. Tombol bulat polos berada di kiri untuk **Light** dan kanan untuk **Dark**. Gunakan warna netral yang mengikuti palet aktif dan bayangan halus pada jalur. Aktivasi sakelar langsung berpindah ke tema sebaliknya, termasuk saat tema sebelumnya mengikuti perangkat. Sakelar dapat dioperasikan dengan keyboard. [Layout Tenant Admin](../../frontend/apps/tenant-admin/src/app/layout/app-layout.html)
- Pilihan manual tetap aktif saat tema perangkat berubah. Pilih **Ikuti Sistem** di menu akun untuk kembali mengikuti tema perangkat. Palet dan posisi sakelar mengikuti perubahan tema perangkat selama halaman terbuka ketika mode tersebut aktif.
- Simpan pilihan mode sebagai preferensi lokal Tenant Admin di browser. Pilihan **Ikuti Sistem** disimpan sebagai mode agar tema kembali ditentukan dari perangkat saat aplikasi dibuka. Jika belum ada pilihan tersimpan, gunakan **Ikuti Sistem**.
- Pertahankan nilai palet **Light** saat ini dan tambahkan nilai palet **Dark** di `theme.css` melalui token yang sama. Terapkan kedua palet pada layout Tenant Admin, termasuk sidebar, header, menu seluler, dan menu akun, serta pada daftar dan detail preview Produk. Gunakan token fokus serta pasangan warna teks dan latar status **Terbit** dan **Draf** dari fondasi tema. [Token warna Tenant Admin](../../frontend/apps/tenant-admin/src/theme.css)

## Testing Decisions

Jalankan `npm run dev:tenant-admin` dari `frontend/` untuk verifikasi di browser. Gunakan pengaturan perangkat atau emulasi browser untuk menguji preferensi terang dan gelap. Jalankan `npm run build:tenant-admin` untuk memeriksa build produksi. Preview Produk memakai data contoh lokal. [Panduan frontend](../../frontend/README.md#menjalankan-dan-membangun-aplikasi)

| ID | Cerita | Kondisi, aksi, dan hasil yang teramati |
| --- | --- | --- |
| **TA-MODE-AT-01** | TA-MODE-01 | Aktifkan sakelar di header pada desktop dengan sidebar lebar dan ringkas, lalu pada layar seluler dengan menu navigasi tertutup. Setiap aktivasi langsung mengganti palet dan memindahkan tombol bulat ke sisi yang sesuai. Kedua ikon tetap terlihat di luar jalur dan warna sakelar mengikuti palet aktif. |
| **TA-MODE-AT-02** | TA-MODE-02 | Dengan preferensi browser kosong, buka Tenant Admin saat tema perangkat terang dan ulangi saat perangkat gelap. Palet dan sakelar mengikuti perangkat. Ubah tema perangkat saat halaman terbuka. Palet dan sakelar ikut berubah selama **Ikuti Sistem** aktif. Ubah tema secara manual, verifikasi bahwa perubahan tema perangkat tidak mengubah palet, lalu aktifkan kembali **Ikuti Sistem** dari menu akun. |
| **TA-MODE-AT-03** | TA-MODE-03 | Pilih setiap mode, muat ulang halaman, lalu buka kembali browser dengan profil yang sama. Mode yang dipilih tetap aktif. Untuk **Ikuti Sistem**, ubah tema perangkat sebelum membuka kembali aplikasi dan periksa bahwa palet mengikuti tema perangkat yang terbaru. |
| **TA-MODE-AT-04** | TA-MODE-01, TA-MODE-04 | Aktifkan sakelar dengan keyboard dan periksa bahwa fokus tetap terlihat setelah tema berganti. Pada kedua palet, buka `/katalog/produk`, cari **Kopi Gayo 250 g**, buka detail, lalu kembali melalui **Daftar Produk**. Periksa label **Terbit** dan **Draf**, teks, serta indikator fokus di header, navigasi, dan Produk. Ulangi pada tampilan seluler. |

## Out of Scope

- Tema Platform Admin dan Storefront.
- Palet khusus per tenant, penyimpanan preferensi di akun, sinkronisasi antarperangkat, API, dan database.
- Perubahan alur Produk dan navigasi Tenant Admin di luar penambahan kontrol **Tema**.

## Further Notes

Spesifikasi ini memperluas cakupan fondasi palet terpusat dengan preferensi tampilan lokal pengguna. [Tenant Admin — Tema Warna dari Satu File](tenant-admin-color-theme.md)
