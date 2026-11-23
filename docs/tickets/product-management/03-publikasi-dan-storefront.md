# Product 03 — Publikasi dan storefront tenant

Spec induk: [Product Management](../../product-management.md)

Status: ready-for-agent

## What to Build

Implementasikan PM-07 dan PM-08. Admin mengubah status publikasi melalui API. Pengunjung membuka daftar dan detail publik pada URL toko, memilih nilai opsi, serta melihat galeri dan status ketersediaan. Gunakan pemetaan hostname atau hostname dan jalur toko dari server, serta baca ketersediaan berdasarkan UUID varian.

## Acceptance Criteria

- [ ] PM-AT-07 — Draft tersembunyi pada daftar dan URL detail publik. Publikasi tanpa harga atau varian memberi `409`. Produk yang memenuhi syarat tampil setelah terbit, hilang saat ditarik, dan tampil lagi setelah diterbitkan ulang. Status yang dikirim ulang menghasilkan keadaan yang sama.
- [ ] PM-AT-08 — Kartu menampilkan nama, harga rupiah dan foto pertama bila ada. Detail menampilkan galeri terurut, opsi, dan kombinasi terdaftar. Galeri mengikuti nilai terpilih pertama yang memiliki media menurut urutan opsi, dengan galeri awal sebagai fallback.
- [ ] PM-AT-08 dan bagian publik IM-AT-08 — Kombinasi terdaftar menampilkan Tersedia atau Habis berdasarkan saldo yang sudah dikonfirmasi. Pilihan belum lengkap dan kombinasi yang tidak disediakan memberi hasil berbeda. Produk habis tetap tampil. Produk tanpa opsi memakai varian bawaan.
- [ ] PM-AT-08 — Daftar dan detail hanya berisi produk terbit dari URL toko yang dipetakan. ID produk A pada URL B, URL yang tidak dipetakan, dan produk Draft memberi `404` yang setara. Verifikasi pemetaan domain dan jalur toko.
- [ ] Bagian publikasi PM-AT-09 — Akun baca-saja bisa membaca detail admin dan perubahan publikasi mendapat `403`. Request perubahan tanpa autentikasi mendapat `401`. Baca ulang status setelah penolakan.
- [ ] Jalankan skenario Bruno dan buka halaman storefront di browser. Uji alur gabungan Draft, penerimaan, terbit, pilihan galeri, serta perubahan status stok. Bagian koreksi IM-AT-08 diverifikasi setelah Inventory 02 terintegrasi.

## Blocked By

- [Product 01](01-katalog-draft-dan-sku.md) dan [Product 02](02-media-dan-tautan-opsi.md) untuk katalog, detail, dan galeri.
- [Inventory 01](../inventory-management/01-penerimaan-dan-saldo.md) untuk kontrak status per varian.
- Pemetaan URL toko aktif dari [User Management](../../user-management.md#login-dan-konteks-tenant), tersedia melalui pemetaan hostname dan jalur toko yang diprovisikan. Verifikasi prasyarat sebelum dispatch.
