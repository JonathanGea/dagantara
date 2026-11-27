# Customer Management — Checkout Tamu v1

> **Status — Disetujui.** Spesifikasi ini menetapkan kebijakan identitas pembeli untuk [DEP-04](../planning/development-dependencies.md#dep-04-guest-checkout-and-customer-identity).

## Problem Statement

Pembeli baru ingin membeli produk di toko tanpa harus mendaftar atau masuk ke akun. Toko memerlukan informasi pembeli untuk menangani setiap pesanan dan memastikan keranjang serta pesanan tetap berada di tenant yang sama.

## Solution

Semua pembelian di Storefront v1 memakai checkout tamu. Pengunjung memilih produk dan mengubah keranjang tanpa login, lalu memberikan informasi pembeli saat checkout. Toko menyimpan identitas dan kontak tamu pada pesanan yang dibuat.

## User Stories

1. Sebagai pengunjung baru, saya bisa menambah Kopi Arabika Gayo ke keranjang, mengubah jumlahnya, dan memesan tanpa mendaftar agar pembelian pertama bisa langsung selesai.
2. Sebagai pengunjung yang berpindah toko, saya ingin pilihan saya tetap berada di toko asal agar keranjang dari satu toko tidak menghasilkan pesanan di toko lain.
3. Sebagai pembeli yang kembali, saya bisa memakai kontak yang sama dengan nama pembeli yang berbeda agar tiap pesanan menyimpan informasi yang saya berikan untuk pembelian itu.

## Implementation Decisions

- **CM-01 — Pembelian tanpa akun.** Cart dan Checkout melayani pengunjung Storefront tanpa autentikasi pembeli. Pengunjung bisa menambah dan mengubah item di keranjang tamu serta mengirim pesanan dengan data checkout yang valid. Pembelian tidak membuat akun pembeli secara otomatis. Akun internal mengikuti aturan [User Management](user-management.md).
- **CM-02 — Kepemilikan keranjang dan pesanan.** Keranjang tamu dimiliki satu tenant. Checkout memeriksa tenant keranjang terhadap konteks toko yang ditentukan server dan hanya membuat pesanan pada tenant yang sama. Tolak penggunaan keranjang lintas tenant tanpa membuat pesanan pada salah satu tenant.
- **CM-03 — Identitas tamu per pesanan.** Simpan pada setiap pesanan identitas dan kontak yang diberikan untuk pesanan itu. Pemakaian ulang nomor HP atau email tidak menautkan pesanan ke profil pelanggan dan tidak memberi akses ke pesanan lain. Pesanan baru tidak mengubah identitas atau kontak yang sudah tersimpan pada pesanan sebelumnya.

## Testing Decisions

Verifikasi melalui API Cart dan Checkout setelah kontraknya tersedia dan penyimpanan pesanan terintegrasi. Jalankan `bru run --env Local` dari `bruno/`. Periksa respons HTTP dan keadaan pesanan yang tersimpan.

| ID | Kondisi dan tindakan | Hasil yang harus teramati |
| --- | --- | --- |
| **CM-AT-01** (CM-01, CM-03) | Tanpa login, tambahkan satu bungkus Kopi Arabika Gayo 250 g di Toko Kopi Gayo, ubah jumlahnya menjadi dua, lalu kirim checkout dengan data valid. | Checkout berhasil dan pesanan tercatat di Toko Kopi Gayo dengan jumlah dua serta identitas dan kontak yang dikirim. Pembelian selesai tanpa pembuatan akun. |
| **CM-AT-02** (CM-02) | Buat keranjang tamu di Toko Kopi Gayo, lalu ajukan checkout menggunakan keranjang tersebut di Toko B. | Permintaan ditolak. Tidak ada pesanan yang dibuat di Toko Kopi Gayo maupun Toko B akibat percobaan tersebut. |
| **CM-AT-03** (CM-03) | Buat dua pesanan tamu di toko yang sama dengan nomor HP yang sama dan nama pembeli yang berbeda. | Kedua pesanan menyimpan nama dan kontak masing-masing. Penyimpanan pesanan kedua tidak mengubah data pesanan pertama atau menautkan keduanya ke satu profil pelanggan. |

## Out of Scope

- Pendaftaran, login, pemulihan akses akun pembeli, dan profil pelanggan lintas pesanan ditunda.
- Identifikasi keranjang ketika pengunjung kembali dan aturan perubahan item dibahas di [Cart Management](cart-management.md).
- Isian wajib checkout dan data alamat dibahas di [Checkout Management](checkout-management.md) dan [Address & Location Management](address-location-management.md).
- Cara tamu mengakses status pesanannya dibahas di [Order Management](order-management.md).

## Further Notes

Terapkan CM-01 dan CM-02 dalam alur Cart dan Checkout, serta CM-03 saat Checkout membuat pesanan melalui Order. Kriteria CM-AT-01 sampai CM-AT-03 menjadi bagian penerimaan integrasi alur tersebut.
