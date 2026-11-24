# Store Settings 02 — Logo toko

Spec induk: [Store Settings](../../specs/store-settings.md)

Status: ready-for-agent

## What to Build

Implementasikan SS-02 dan bagian logo dari SS-03, SS-04, dan SS-05. Lengkapi form dan API admin dengan `KEEP`, `REPLACE`, dan `REMOVE`. Validasi satu gambar PNG, JPEG, atau WebP hingga 2 MiB berdasarkan isi. Simpan file secara lokal dan terbitkan referensi aktif bersama nama dan kontak dalam satu transaksi. Sajikan logo aktif pada URL toko dan perbarui header daftar serta detail.

## Acceptance Criteria

- [ ] SS-AT-03 — Unggah logo valid dan periksa tampilan pada daftar dan detail. `KEEP` mempertahankan URL logo. Logo tetap tersedia setelah restart dengan direktori yang sama. Penggantian dan penghapusan menonaktifkan URL lama. Jika file aktif hilang, endpoint memberi `404` dan header tetap menampilkan nama toko.
- [ ] SS-AT-04 — File yang tidak valid menghasilkan `400`, dan gambar di atas 2 MiB menghasilkan `413`. Kontak tidak valid dan kegagalan penyimpanan file mempertahankan semua nilai lama serta logo aktif. Pesan error tidak mengungkap lokasi file.
- [ ] Bagian logo SS-AT-06 — Logo A dan B hanya tersedia melalui URL tenant masing-masing. ID logo A pada URL B atau URL yang tidak terpetakan mendapat `404`.
- [ ] Kontrak aksi logo menolak file pada `KEEP` atau `REMOVE` dan mewajibkan satu file untuk `REPLACE`. Setelah perubahan bersamaan, hanya referensi aktif dapat diakses dan pembersihan tidak menghapus file aktif.
- [ ] Jalankan Bruno dengan `bru run --env Local`, tes integrasi kegagalan penyimpanan, dan verifikasi browser terhadap kedua halaman serta setelah restart. Catat perintah dan hasil per skenario.

## Blocked By

- [Store Settings 01](01-nama-dan-kontak-publik.md) menyediakan form, permission, penyimpanan pengaturan per tenant, dan header storefront.
