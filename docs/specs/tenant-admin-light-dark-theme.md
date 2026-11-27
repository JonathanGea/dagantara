# Tenant Admin — Tema Light, Dark, dan Ikuti Sistem

## Problem Statement

Tenant Admin selalu tampil dengan palet terang. Pengguna membutuhkan pilihan tampilan untuk bekerja dengan palet gelap atau mengikuti tema perangkat.

## Solution

Pengguna memilih **Light**, **Dark**, atau **Ikuti Sistem** melalui kontrol **Tema** di menu akun Tenant Admin. Pilihan langsung diterapkan dan diingat oleh browser. Kunjungan pertama mengikuti tema perangkat.

## User Stories

1. **TA-MODE-01** — Sebagai pengguna Tenant Admin, saya bisa memilih tema dari menu akun di desktop dan seluler agar tampilan segera sesuai preferensi saya.
2. **TA-MODE-02** — Sebagai pengguna yang memilih **Ikuti Sistem**, saya ingin tampilan mengikuti tema perangkat saat pertama dibuka dan saat tema perangkat berubah.
3. **TA-MODE-03** — Sebagai pengguna Tenant Admin, saya ingin pilihan tema tetap aktif setelah halaman dimuat ulang atau browser dibuka kembali.
4. **TA-MODE-04** — Sebagai pengguna Tenant Admin, saya ingin teks, label status produk, dan indikator fokus keyboard tetap terbaca pada kedua palet.

## Implementation Decisions

- Letakkan kontrol **Tema** dalam menu akun di bagian bawah sidebar. Gunakan menu akun yang sama saat sidebar desktop diringkas dan saat sidebar dibuka di layar seluler. Tampilkan pilihan yang aktif dan pastikan ketiganya bisa dipilih dengan keyboard. [Layout Tenant Admin](../../frontend/apps/tenant-admin/src/app/layout/app-layout.html)
- Terapkan **Light** dan **Dark** secara langsung. Kedua pilihan manual tetap aktif saat tema perangkat berubah. Saat **Ikuti Sistem** dipilih, gunakan tema perangkat saat itu dan ikuti perubahan berikutnya selama halaman terbuka.
- Simpan pilihan mode sebagai preferensi lokal Tenant Admin di browser. Pilihan **Ikuti Sistem** disimpan sebagai mode agar tema kembali ditentukan dari perangkat saat aplikasi dibuka. Jika belum ada pilihan tersimpan, gunakan **Ikuti Sistem**.
- Pertahankan nilai palet **Light** saat ini dan tambahkan nilai palet **Dark** di `theme.css` melalui token yang sama. Terapkan kedua palet pada layout Tenant Admin, termasuk sidebar, header, menu seluler, dan menu akun, serta pada daftar dan detail preview Produk. Gunakan token fokus serta pasangan warna teks dan latar status **Terbit** dan **Draf** dari fondasi tema. [Token warna Tenant Admin](../../frontend/apps/tenant-admin/src/theme.css)

## Testing Decisions

Jalankan `npm run dev:tenant-admin` dari `frontend/` untuk verifikasi di browser. Gunakan pengaturan perangkat atau emulasi browser untuk menguji preferensi terang dan gelap. Jalankan `npm run build:tenant-admin` untuk memeriksa build produksi. Preview Produk memakai data contoh lokal. [Panduan frontend](../../frontend/README.md#menjalankan-dan-membangun-aplikasi)

| ID | Cerita | Kondisi, aksi, dan hasil yang teramati |
| --- | --- | --- |
| **TA-MODE-AT-01** | TA-MODE-01 | Buka menu akun dari sidebar desktop dalam keadaan terbuka dan ringkas, lalu dari menu seluler. Pilih **Light**, **Dark**, dan **Ikuti Sistem**. Kontrol menunjukkan pilihan aktif dan palet berubah tanpa memuat ulang halaman. Palet **Light** sesuai tampilan awal Tenant Admin. |
| **TA-MODE-AT-02** | TA-MODE-02 | Dengan preferensi browser kosong, buka Tenant Admin saat tema perangkat terang dan ulangi saat perangkat gelap. **Ikuti Sistem** terpilih dan palet mengikuti perangkat. Ubah tema perangkat saat halaman terbuka. Palet ikut berubah dalam mode **Ikuti Sistem**. Ulangi saat **Light** dan **Dark** dipilih. Palet mengikuti pilihan manual. |
| **TA-MODE-AT-03** | TA-MODE-03 | Pilih setiap mode, muat ulang halaman, lalu buka kembali browser dengan profil yang sama. Mode yang dipilih tetap aktif. Untuk **Ikuti Sistem**, ubah tema perangkat sebelum membuka kembali aplikasi dan periksa bahwa palet mengikuti tema perangkat yang terbaru. |
| **TA-MODE-AT-04** | TA-MODE-01, TA-MODE-04 | Pada kedua palet, buka `/katalog/produk`, cari **Kopi Gayo 250 g**, buka detail, lalu kembali melalui **Daftar Produk**. Periksa label **Terbit** dan **Draf**, teks, serta indikator fokus saat memakai keyboard di menu akun, navigasi, dan Produk. Ulangi pada tampilan seluler. |

## Out of Scope

- Tema Platform Admin dan Storefront.
- Palet khusus per tenant, penyimpanan preferensi di akun, sinkronisasi antarperangkat, API, dan database.
- Perubahan alur Produk dan navigasi Tenant Admin di luar penambahan kontrol **Tema**.

## Further Notes

Spesifikasi ini memperluas cakupan fondasi palet terpusat dengan preferensi tampilan lokal pengguna. [Tenant Admin — Tema Warna dari Satu File](tenant-admin-color-theme.md)
