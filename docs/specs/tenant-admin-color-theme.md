# Tenant Admin — Tema Warna dari Satu File

## Problem Statement

Developer perlu mencari warna di beberapa stylesheet untuk memperbarui palet Tenant Admin. Layout sudah punya beberapa variabel CSS, sementara warna latar, interaksi, dan label status sebagian masih ditulis langsung di komponen.

## Solution

Developer mengatur palet dari satu file `theme.css`. Layout serta halaman daftar dan detail pada preview Produk memakai token sesuai fungsi warna. Tema bawaan memakai palet Tenant Admin saat ini.

## User Stories

1. **TA-THEME-01** — Sebagai developer, saya bisa mengganti palet dari satu file dan melihat hasilnya pada layout serta preview Produk.
2. **TA-THEME-02** — Sebagai pengguna Tenant Admin, saya tetap bisa membaca teks, mengenali status produk, dan melihat fokus saat menggunakan keyboard setelah palet diubah.

## Implementation Decisions

- Tambahkan `theme.css` di `frontend/apps/tenant-admin/src/`. Muat file tersebut dari `styles.css` dan deklarasikan token sebagai CSS custom properties pada `:root` Tenant Admin.
- Gunakan token sesuai fungsi warna. Mulai dari `--color-page`, `--color-surface`, `--color-text`, `--color-text-muted`, `--color-border`, `--color-primary`, dan `--color-focus`. Gunakan token untuk warna sidebar, navigasi aktif, dan keadaan hover sesuai kebutuhan tampilan sekarang. Komponen dan utility class dalam cakupan membaca nilai dari token yang sama.
- Gunakan nilai `--color-primary` untuk aksen aksi yang sudah ada, seperti teks aksi **Daftar Produk**. Evaluasi keterbacaan teks aksi terhadap latarnya setiap kali palet berubah. Jika ada aksi yang memakai warna utama sebagai latar, pasangkan dengan `--color-on-primary` untuk teksnya.
- Gunakan pasangan token latar dan teks tersendiri untuk label **Terbit** dan **Draf**. Pertahankan teks kedua status dan atur warnanya secara terpisah dari aksen aksi.
- Terapkan token pada warna di layout Tenant Admin, termasuk sidebar, header, dan menu seluler, serta pada daftar dan detail preview Produk. Ambil nilai bawaan dari stylesheet yang ada. [Layout Tenant Admin](../../frontend/apps/tenant-admin/src/app/layout/app-layout.css) · [Preview Produk](../../frontend/apps/tenant-admin/src/app/features/product-management/product-preview.css)

## Testing Decisions

Jalankan `npm run dev:tenant-admin` dari `frontend/` untuk verifikasi di browser dan `npm run build:tenant-admin` untuk memeriksa build produksi. Preview Produk memakai data contoh lokal. [Panduan frontend](../../frontend/README.md#menjalankan-dan-membangun-aplikasi)

| ID | Cerita | Kondisi, aksi, dan hasil yang teramati |
| --- | --- | --- |
| **TA-THEME-AT-01** | TA-THEME-01 | Dengan tema bawaan, buka `/katalog/produk`, lalu pilih **Kopi Gayo 250 g**. Bandingkan dengan tampilan sebelum perubahan. Warna layout, daftar, dan detail sesuai palet awal. Pencarian produk, navigasi detail, dan aksi **Daftar Produk** tetap bekerja. |
| **TA-THEME-AT-02** | TA-THEME-01 | Ubah sementara latar halaman, permukaan kartu, aksen aksi, dan fokus hanya di `theme.css`. Muat ulang daftar dan detail. Periksa bahwa layout dan preview mengikuti nilai baru, termasuk aksi **Daftar Produk** pada halaman detail. Kembalikan palet bawaan setelah verifikasi. |
| **TA-THEME-AT-03** | TA-THEME-02 | Cek palet bawaan dan palet alternatif pada layout desktop dan menu seluler. Gunakan `Tab` melalui navigasi, pencarian produk, dan tautan produk. Indikator fokus terlihat pada latarnya dan teks utama, teks sekunder, serta teks aksi terbaca. Saat aksen aksi diganti, warna label **Terbit** dan **Draf** tetap mengikuti token status masing-masing dan kedua label terbaca. |

## Out of Scope

- Pemilih tema, pengaturan tema per tenant, dan penyimpanan palet melalui API atau database.
- Palet alternatif sebagai tema bawaan tambahan, mode gelap, dan perubahan tema Platform Admin atau Storefront.
- Perubahan layout, alur produk, dan token tipografi atau jarak.

## Further Notes

- Perubahan ini mendukung pekerjaan pola UI pada fondasi frontend. Perluasan untuk pemilih tema dan mode gelap ditetapkan dalam spesifikasi lanjutan. [Fondasi Angular dan Tailwind](../planning/progress.md#fondasi-angular-dan-tailwind) · [Tema Light, Dark, dan Ikuti Sistem](tenant-admin-light-dark-theme.md)
- Spesifikasi Store Settings tetap menjadi rujukan pengaturan identitas publik toko. [Store Settings](store-settings.md)
