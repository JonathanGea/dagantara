# Progres Modul Dagantara

Selesaikan spesifikasi alur bisnis dan kode backend terlebih dahulu. Setelah fase backend dalam cakupan yang disepakati selesai, buka [sesi frontend dan desain UI](#frontend-dan-desain-ui) dengan spesifikasi backend sebagai konteks.

## Antrean backend

| Status | Pekerjaan | Langkah berikutnya atau prasyarat |
| --- | --- | --- |
| **Siap dikerjakan** | [API Product Discovery v1](#product-catalog--discovery) | Implementasikan pencarian dan filter kategori dengan kontrak publikasi dan stok yang tersedia. Verifikasi `PD-AT-01` sampai `PD-AT-06` dengan Bruno. |
| **Diskusi selanjutnya** | [Customer Management](#customer-management) | Putuskan checkout tamu atau wajib login ([DEP-04](development-dependencies.md#open-decisions--blockers)). Catat hasilnya untuk Cart dan Checkout. |
| **Menunggu keputusan identitas** | [Cart Management](#cart-management) | Setelah DEP-04 disepakati, bahas cara mengenali keranjang yang dibuka kembali dan aturan item saat produk, varian, atau stok berubah (DEP-02). |
| **Perlu keputusan bersama** | [Checkout](#checkout-management), [Order](#order-management), dan [reservasi stok](#inventory-management) | Selaraskan identitas pembeli, Cart, snapshot harga, status awal Order, dan waktu reservasi. Penerimaan Checkout terintegrasi juga memerlukan kontrak pilihan Payment dan Shipping. |
| **Menunggu kontrak Order** | [Payment](#payment-management) dan [Shipping](#shipping-management) | Setelah identitas dan status Order disepakati (DEP-06), implementasi transfer manual dan pemenuhan bisa maju paralel sesuai kontrak masing-masing. |
| **Menunggu konteks alamat** | [Address & Location](#address--location-management) | Bahas data minimum alamat dan asal kirim bersama Shipping setelah identitas pembeli serta kepemilikan alamat jelas (DEP-04 dan DEP-05). |

API Product Discovery bisa dikerjakan bersamaan dengan diskusi Customer Management. Cart bisa lanjut setelah keputusan identitas tercatat. Checkout dan Order bisa mulai dikerjakan paralel setelah kontrak bersama disetujui. Lihat [roadmap](module-roadmap.md#module-matrix) untuk prioritas dan [dependensi pengembangan](development-dependencies.md#open-decisions--blockers) untuk rincian keputusan terbuka.

Pada tahap backend, bahas kebutuhan pembeli dan admin yang menentukan data, izin, validasi, status, atau respons sistem. Contohnya, data wajib untuk membuat pesanan dibahas di Checkout. Susunan form dan jumlah layar dibahas di sesi desain UI.

### Cara memperbarui status backend

- Centang **Spec selesai** setelah spesifikasi backend untuk versi yang dilacak disetujui dan ditulis. Centang **Kode selesai** setelah cakupan backend diimplementasikan dan verifikasi yang berlaku lulus. Tautkan spec dan bukti penyelesaian dari entri modul.
- Jalankan penerimaan API HTTP melalui Bruno dengan `bru run --env Local` dari `bruno/`. Pertahankan pemeriksaan pada halaman yang sudah tersedia jika spec memakainya untuk membuktikan perilaku bisnis, seperti penutupan akses katalog tenant nonaktif.
- Pindahkan penanda **Berikutnya dibahas** setelah spec modul disetujui dan ditulis. Buka **Prompt spec** untuk melanjutkan draf dan **Prompt implementasi** setelah spec serta cakupan pengerjaan disetujui.
- Ikuti [langkah regenerasi viewer](../README.md#regenerasi-viewer) setelah memperbarui progres.

## Backend MVP

### User Management

- [x] Spec selesai — [Akses Platform dan Tenant](../specs/user-management.md)
- [x] Kode selesai — [lima tiket implementasi](../specs/user-management.md#implementation-tickets)

<details>
<summary>Prompt User Management</summary>

```text
$write-module-spec

Bahas perubahan berikutnya untuk User Management. Baca docs/specs/user-management.md dan tiket terkait. Mulai dari kebutuhan yang ingin saya bahas serta dampaknya pada akun internal, role, dan izin.
```

</details>

### Product Management

- [x] Spec selesai — [Product Management](../specs/product-management.md)
- [x] Kode selesai — [tiga tiket implementasi](../specs/product-management.md#implementation-tickets)

<details>
<summary>Prompt Product Management</summary>

```text
$write-module-spec

Bahas perubahan berikutnya untuk Product Management. Baca docs/specs/product-management.md. Mulai dari kasus pengelolaan katalog yang ingin saya perbaiki dan cek aturan kategori, varian, serta SKU yang terdampak.
```

</details>

### Tenant Management

- [x] Spec selesai — [Penonaktifan dan Aktivasi Kembali Tenant](../specs/tenant-management.md). [Penggantian URL toko](../specs/tenant-management.md#further-notes) dicatat sebagai enhancement.
- [x] Kode selesai — [TM-AT-01 sampai TM-AT-05 lulus di Bruno dan browser](../../bruno/tenant-management/README.md#hasil-verifikasi).

<details>
<summary>Prompt implementasi Tenant Management</summary>

```text
$implement-from-spec tenant-management

Implementasikan Tenant Management v1 sesuai docs/specs/tenant-management.md.
Verifikasi TM-AT-01 sampai TM-AT-04 dengan Bruno dan TM-AT-05 dengan browser.
Perbarui Kode selesai di Progres Modul setelah seluruh cakupan terverifikasi.
```

</details>

### Store Settings

- [x] Spec selesai — [Identitas Publik Toko](../specs/store-settings.md)
- [x] Kode selesai — [dua tiket implementasi](../specs/store-settings.md#implementation-tickets) · [verifikasi gabungan](../tickets/store-settings/02-logo-toko.md#verifikasi-gabungan)

<details>
<summary>Prompt Store Settings</summary>

```text
$write-module-spec

Bahas perubahan berikutnya untuk Store Settings. Baca docs/specs/store-settings.md dan
dua tiket yang sudah selesai. Mulai dari kebutuhan yang ingin saya bahas serta dampaknya
pada pengaturan tenant dan identitas toko yang ditampilkan ke pengunjung.
```

</details>

### Inventory Management

- [x] Spec selesai — [Inventory Management](../specs/inventory-management.md)
- [x] Kode selesai — [dua tiket implementasi](../specs/inventory-management.md#implementation-tickets)

<details>
<summary>Prompt Inventory Management</summary>

```text
$write-module-spec

Bahas lanjutan Inventory Management untuk reservasi stok. Baca docs/specs/inventory-management.md dan DEP-03. Mulai dari satu keputusan. Pada langkah apa stok mulai ditahan untuk sebuah pesanan?
```

</details>

### Product Catalog / Discovery

- [x] Spec selesai — [Pencarian dan Filter Produk Publik](../specs/product-discovery.md)
- [ ] Kode selesai — API dan verifikasi Bruno. Lihat [sesi katalog publik](#urutan-sesi-desain-lintas-modul) untuk pekerjaan frontend.

<details>
<summary>Prompt implementasi Product Catalog / Discovery</summary>

```text
$implement-from-spec product-discovery

Implementasikan API pencarian nama dan filter kategori sesuai docs/specs/product-discovery.md. Verifikasi PD-AT-01 sampai PD-AT-06 melalui Bruno. Pekerjaan frontend dan skenario browser berada pada tahap lanjutan.
```

</details>

### Customer Management

**Berikutnya dibahas.** Apakah pembeli bisa checkout sebagai tamu atau perlu login? [DEP-04](development-dependencies.md#open-decisions--blockers)

- [ ] Spec selesai — [Draf spec](../specs/customer-management.md)
- [ ] Kode selesai

<details>
<summary>Prompt spec Customer Management</summary>

```text
$write-module-spec

Gunakan docs/specs/customer-management.md sebagai kerangka awal.
Bahas Customer Management dari DEP-04. Jika pengunjung baru ingin membeli Kopi Arabika
Gayo, bolehkah dia checkout sebagai tamu atau perlu login? Beri rekomendasi untuk versi
pertama, contoh konkret, dan konsekuensinya bagi keranjang dan checkout. Tunggu jawaban
saya.

Ajukan satu keputusan per putaran dengan rekomendasi dan contoh konkret. Tunggu jawaban
saya sebelum lanjut dan catat tambahan yang disepakati.

Rangkum cakupan dan kriteria penerimaan untuk saya konfirmasi. Setelah saya setuju,
lengkapi draf yang sama sesuai template skill, tautkan spec dari docs/README.md, dan
perbarui Progres Modul.
```

</details>

<details>
<summary>Prompt implementasi Customer Management</summary>

```text
$implement-from-spec customer-management

Baca docs/specs/customer-management.md dan cek status modul di docs/planning/progress.md.
Mulai implementasi setelah spec dan cakupan pengerjaan disetujui. Jika spec masih draf
atau tiket masih menunggu persetujuan, laporkan prasyarat yang tertahan.

Kerjakan cakupan yang disetujui dan jalankan kriteria penerimaannya, termasuk Bruno jika
ada API HTTP. Perbarui Kode selesai di Progres Modul setelah seluruh cakupan versi yang
dilacak terverifikasi.
```

</details>

### Cart Management

- [ ] Spec selesai — [Draf spec](../specs/cart-management.md)
- [ ] Kode selesai

<details>
<summary>Prompt spec Cart Management</summary>

```text
$write-module-spec

Gunakan docs/specs/cart-management.md sebagai kerangka awal.
Bahas Cart Management dengan merujuk keputusan Customer Management dan DEP-02. Mulai dari
satu keputusan. Bagaimana toko mengenali keranjang pembeli ketika ia kembali membuka URL
toko?

Ajukan satu keputusan per putaran dengan rekomendasi dan contoh konkret. Tunggu jawaban
saya sebelum lanjut dan catat tambahan yang disepakati.

Rangkum cakupan dan kriteria penerimaan untuk saya konfirmasi. Setelah saya setuju,
lengkapi draf yang sama sesuai template skill, tautkan spec dari docs/README.md, dan
perbarui Progres Modul.
```

</details>

<details>
<summary>Prompt implementasi Cart Management</summary>

```text
$implement-from-spec cart-management

Baca docs/specs/cart-management.md dan cek status modul di docs/planning/progress.md.
Mulai implementasi setelah spec dan cakupan pengerjaan disetujui. Jika spec masih draf
atau tiket masih menunggu persetujuan, laporkan prasyarat yang tertahan.

Kerjakan cakupan yang disetujui dan jalankan kriteria penerimaannya, termasuk Bruno jika
ada API HTTP. Perbarui Kode selesai di Progres Modul setelah seluruh cakupan versi yang
dilacak terverifikasi.
```

</details>

### Checkout Management

- [ ] Spec selesai — [Draf spec](../specs/checkout-management.md)
- [ ] Kode selesai

<details>
<summary>Prompt spec Checkout Management</summary>

```text
$write-module-spec

Gunakan docs/specs/checkout-management.md sebagai kerangka awal.
Bahas Checkout Management dengan merujuk keputusan Customer, Cart, Payment, dan Shipping
yang sudah tersedia. Mulai dari satu keputusan. Data apa yang wajib diisi pembeli sebelum
bisa membuat pesanan?

Ajukan satu keputusan per putaran dengan rekomendasi dan contoh konkret. Tunggu jawaban
saya sebelum lanjut dan catat tambahan yang disepakati.

Rangkum cakupan dan kriteria penerimaan untuk saya konfirmasi. Setelah saya setuju,
lengkapi draf yang sama sesuai template skill, tautkan spec dari docs/README.md, dan
perbarui Progres Modul.
```

</details>

<details>
<summary>Prompt implementasi Checkout Management</summary>

```text
$implement-from-spec checkout-management

Baca docs/specs/checkout-management.md dan cek status modul di docs/planning/progress.md.
Mulai implementasi setelah spec dan cakupan pengerjaan disetujui. Jika spec masih draf
atau tiket masih menunggu persetujuan, laporkan prasyarat yang tertahan.

Kerjakan cakupan yang disetujui dan jalankan kriteria penerimaannya, termasuk Bruno jika
ada API HTTP. Perbarui Kode selesai di Progres Modul setelah seluruh cakupan versi yang
dilacak terverifikasi.
```

</details>

### Order Management

- [ ] Spec selesai — [Draf spec](../specs/order-management.md)
- [ ] Kode selesai

<details>
<summary>Prompt spec Order Management</summary>

```text
$write-module-spec

Gunakan docs/specs/order-management.md sebagai kerangka awal.
Bahas Order Management dari DEP-06 dan alur Checkout. Mulai dari satu keputusan. Apa
status awal pesanan ketika pembeli berhasil checkout?

Ajukan satu keputusan per putaran dengan rekomendasi dan contoh konkret. Tunggu jawaban
saya sebelum lanjut dan catat tambahan yang disepakati.

Rangkum cakupan dan kriteria penerimaan untuk saya konfirmasi. Setelah saya setuju,
lengkapi draf yang sama sesuai template skill, tautkan spec dari docs/README.md, dan
perbarui Progres Modul.
```

</details>

<details>
<summary>Prompt implementasi Order Management</summary>

```text
$implement-from-spec order-management

Baca docs/specs/order-management.md dan cek status modul di docs/planning/progress.md.
Mulai implementasi setelah spec dan cakupan pengerjaan disetujui. Jika spec masih draf
atau tiket masih menunggu persetujuan, laporkan prasyarat yang tertahan.

Kerjakan cakupan yang disetujui dan jalankan kriteria penerimaannya, termasuk Bruno jika
ada API HTTP. Perbarui Kode selesai di Progres Modul setelah seluruh cakupan versi yang
dilacak terverifikasi.
```

</details>

### Payment Management

- [ ] Spec selesai — [Draf spec](../specs/payment-management.md)
- [ ] Kode selesai

<details>
<summary>Prompt spec Payment Management</summary>

```text
$write-module-spec

Gunakan docs/specs/payment-management.md sebagai kerangka awal.
Bahas Payment Management versi transfer bank manual dengan merujuk status Order. Mulai
dari satu keputusan. Bukti apa yang perlu diberikan pembeli agar admin bisa memverifikasi
pembayaran?

Ajukan satu keputusan per putaran dengan rekomendasi dan contoh konkret. Tunggu jawaban
saya sebelum lanjut dan catat tambahan yang disepakati.

Rangkum cakupan dan kriteria penerimaan untuk saya konfirmasi. Setelah saya setuju,
lengkapi draf yang sama sesuai template skill, tautkan spec dari docs/README.md, dan
perbarui Progres Modul.
```

</details>

<details>
<summary>Prompt implementasi Payment Management</summary>

```text
$implement-from-spec payment-management

Baca docs/specs/payment-management.md dan cek status modul di docs/planning/progress.md.
Mulai implementasi setelah spec dan cakupan pengerjaan disetujui. Jika spec masih draf
atau tiket masih menunggu persetujuan, laporkan prasyarat yang tertahan.

Kerjakan cakupan yang disetujui dan jalankan kriteria penerimaannya, termasuk Bruno jika
ada API HTTP. Perbarui Kode selesai di Progres Modul setelah seluruh cakupan versi yang
dilacak terverifikasi.
```

</details>

### Shipping Management

- [ ] Spec selesai — [Draf spec](../specs/shipping-management.md)
- [ ] Kode selesai

<details>
<summary>Prompt spec Shipping Management</summary>

```text
$write-module-spec

Gunakan docs/specs/shipping-management.md sebagai kerangka awal.
Bahas Shipping Management dari DEP-05. Mulai dari satu keputusan. Bagaimana toko
menghitung ongkir yang ditampilkan sebelum pembeli membuat pesanan?

Ajukan satu keputusan per putaran dengan rekomendasi dan contoh konkret. Tunggu jawaban
saya sebelum lanjut dan catat tambahan yang disepakati.

Rangkum cakupan dan kriteria penerimaan untuk saya konfirmasi. Setelah saya setuju,
lengkapi draf yang sama sesuai template skill, tautkan spec dari docs/README.md, dan
perbarui Progres Modul.
```

</details>

<details>
<summary>Prompt implementasi Shipping Management</summary>

```text
$implement-from-spec shipping-management

Baca docs/specs/shipping-management.md dan cek status modul di docs/planning/progress.md.
Mulai implementasi setelah spec dan cakupan pengerjaan disetujui. Jika spec masih draf
atau tiket masih menunggu persetujuan, laporkan prasyarat yang tertahan.

Kerjakan cakupan yang disetujui dan jalankan kriteria penerimaannya, termasuk Bruno jika
ada API HTTP. Perbarui Kode selesai di Progres Modul setelah seluruh cakupan versi yang
dilacak terverifikasi.
```

</details>

### Address & Location Management

- [ ] Spec selesai — [Draf spec](../specs/address-location-management.md)
- [ ] Kode selesai

<details>
<summary>Prompt spec Address &amp; Location Management</summary>

```text
$write-module-spec

Gunakan docs/specs/address-location-management.md sebagai kerangka awal.
Bahas Address & Location Management dengan merujuk kebutuhan Customer dan Shipping. Mulai
dari satu keputusan. Data alamat tujuan minimum apa yang diperlukan agar pesanan bisa
dikirim?

Ajukan satu keputusan per putaran dengan rekomendasi dan contoh konkret. Tunggu jawaban
saya sebelum lanjut dan catat tambahan yang disepakati.

Rangkum cakupan dan kriteria penerimaan untuk saya konfirmasi. Setelah saya setuju,
lengkapi draf yang sama sesuai template skill, tautkan spec dari docs/README.md, dan
perbarui Progres Modul.
```

</details>

<details>
<summary>Prompt implementasi Address &amp; Location Management</summary>

```text
$implement-from-spec address-location-management

Baca docs/specs/address-location-management.md dan cek status modul di
docs/planning/progress.md. Mulai implementasi setelah spec dan cakupan pengerjaan
disetujui. Jika spec masih draf atau tiket masih menunggu persetujuan, laporkan prasyarat
yang tertahan.

Kerjakan cakupan yang disetujui dan jalankan kriteria penerimaannya, termasuk Bruno jika
ada API HTTP. Perbarui Kode selesai di Progres Modul setelah seluruh cakupan versi yang
dilacak terverifikasi.
```

</details>

## Backend pasca-MVP

### Pricing & Promotion

- [ ] Spec selesai — [Draf spec](../specs/pricing-promotion.md)
- [ ] Kode selesai

<details>
<summary>Prompt spec Pricing &amp; Promotion</summary>

```text
$write-module-spec

Gunakan docs/specs/pricing-promotion.md sebagai kerangka awal.
Bahas Pricing & Promotion dengan merujuk DEP-07 dan kontrak harga pada pesanan. Mulai dari
satu keputusan. Jenis promosi apa yang perlu didukung pada versi pertama?

Ajukan satu keputusan per putaran dengan rekomendasi dan contoh konkret. Tunggu jawaban
saya sebelum lanjut dan catat tambahan yang disepakati.

Rangkum cakupan dan kriteria penerimaan untuk saya konfirmasi. Setelah saya setuju,
lengkapi draf yang sama sesuai template skill, tautkan spec dari docs/README.md, dan
perbarui Progres Modul.
```

</details>

<details>
<summary>Prompt implementasi Pricing &amp; Promotion</summary>

```text
$implement-from-spec pricing-promotion

Baca docs/specs/pricing-promotion.md dan cek status modul di docs/planning/progress.md.
Mulai implementasi setelah spec dan cakupan pengerjaan disetujui. Jika spec masih draf
atau tiket masih menunggu persetujuan, laporkan prasyarat yang tertahan.

Kerjakan cakupan yang disetujui dan jalankan kriteria penerimaannya, termasuk Bruno jika
ada API HTTP. Perbarui Kode selesai di Progres Modul setelah seluruh cakupan versi yang
dilacak terverifikasi.
```

</details>

### Notification Management

- [ ] Spec selesai — [Draf spec](../specs/notification-management.md)
- [ ] Kode selesai

<details>
<summary>Prompt spec Notification Management</summary>

```text
$write-module-spec

Gunakan docs/specs/notification-management.md sebagai kerangka awal.
Bahas Notification Management dengan merujuk alur Order dan Payment. Mulai dari satu
keputusan. Setelah pesanan dibuat, pemberitahuan apa yang paling perlu diterima pembeli?

Ajukan satu keputusan per putaran dengan rekomendasi dan contoh konkret. Tunggu jawaban
saya sebelum lanjut dan catat tambahan yang disepakati.

Rangkum cakupan dan kriteria penerimaan untuk saya konfirmasi. Setelah saya setuju,
lengkapi draf yang sama sesuai template skill, tautkan spec dari docs/README.md, dan
perbarui Progres Modul.
```

</details>

<details>
<summary>Prompt implementasi Notification Management</summary>

```text
$implement-from-spec notification-management

Baca docs/specs/notification-management.md dan cek status modul di
docs/planning/progress.md. Mulai implementasi setelah spec dan cakupan pengerjaan
disetujui. Jika spec masih draf atau tiket masih menunggu persetujuan, laporkan prasyarat
yang tertahan.

Kerjakan cakupan yang disetujui dan jalankan kriteria penerimaannya, termasuk Bruno jika
ada API HTTP. Perbarui Kode selesai di Progres Modul setelah seluruh cakupan versi yang
dilacak terverifikasi.
```

</details>

### Reporting & Analytics

- [ ] Spec selesai — [Draf spec](../specs/reporting-analytics.md)
- [ ] Kode selesai

<details>
<summary>Prompt spec Reporting &amp; Analytics</summary>

```text
$write-module-spec

Gunakan docs/specs/reporting-analytics.md sebagai kerangka awal.
Bahas Reporting & Analytics dengan melihat data pesanan, pembayaran, dan stok yang
tersedia per tenant. Mulai dari satu keputusan. Keputusan apa yang ingin dibantu oleh
laporan pertama untuk pengelola toko?

Ajukan satu keputusan per putaran dengan rekomendasi dan contoh konkret. Tunggu jawaban
saya sebelum lanjut dan catat tambahan yang disepakati.

Rangkum cakupan dan kriteria penerimaan untuk saya konfirmasi. Setelah saya setuju,
lengkapi draf yang sama sesuai template skill, tautkan spec dari docs/README.md, dan
perbarui Progres Modul.
```

</details>

<details>
<summary>Prompt implementasi Reporting &amp; Analytics</summary>

```text
$implement-from-spec reporting-analytics

Baca docs/specs/reporting-analytics.md dan cek status modul di docs/planning/progress.md.
Mulai implementasi setelah spec dan cakupan pengerjaan disetujui. Jika spec masih draf
atau tiket masih menunggu persetujuan, laporkan prasyarat yang tertahan.

Kerjakan cakupan yang disetujui dan jalankan kriteria penerimaannya, termasuk Bruno jika
ada API HTTP. Perbarui Kode selesai di Progres Modul setelah seluruh cakupan versi yang
dilacak terverifikasi.
```

</details>

### Tax Management

- [ ] Spec selesai — [Draf spec](../specs/tax-management.md)
- [ ] Kode selesai

<details>
<summary>Prompt spec Tax Management</summary>

```text
$write-module-spec

Gunakan docs/specs/tax-management.md sebagai kerangka awal.
Bahas Tax Management dari DEP-07. Mulai dari satu keputusan bisnis. Pada tahap mana
backend perlu menyediakan rincian pajak bagi pembeli pada alur checkout?

Ajukan satu keputusan per putaran dengan rekomendasi dan contoh konkret. Tunggu jawaban
saya sebelum lanjut dan catat tambahan yang disepakati.

Rangkum cakupan dan kriteria penerimaan untuk saya konfirmasi. Setelah saya setuju,
lengkapi draf yang sama sesuai template skill, tautkan spec dari docs/README.md, dan
perbarui Progres Modul.
```

</details>

<details>
<summary>Prompt implementasi Tax Management</summary>

```text
$implement-from-spec tax-management

Baca docs/specs/tax-management.md dan cek status modul di docs/planning/progress.md. Mulai
implementasi setelah spec dan cakupan pengerjaan disetujui. Jika spec masih draf atau
tiket masih menunggu persetujuan, laporkan prasyarat yang tertahan.

Kerjakan cakupan yang disetujui dan jalankan kriteria penerimaannya, termasuk Bruno jika
ada API HTTP. Perbarui Kode selesai di Progres Modul setelah seluruh cakupan versi yang
dilacak terverifikasi.
```

</details>

### Platform Configuration

- [ ] Spec selesai — [Draf spec](../specs/platform-configuration.md)
- [ ] Kode selesai

<details>
<summary>Prompt spec Platform Configuration</summary>

```text
$write-module-spec

Gunakan docs/specs/platform-configuration.md sebagai kerangka awal.
Bahas Platform Configuration dengan merujuk aturan akun dan izin platform. Mulai dari satu
keputusan. Pengaturan global apa yang pertama kali perlu bisa diubah oleh operator
platform?

Ajukan satu keputusan per putaran dengan rekomendasi dan contoh konkret. Tunggu jawaban
saya sebelum lanjut dan catat tambahan yang disepakati.

Rangkum cakupan dan kriteria penerimaan untuk saya konfirmasi. Setelah saya setuju,
lengkapi draf yang sama sesuai template skill, tautkan spec dari docs/README.md, dan
perbarui Progres Modul.
```

</details>

<details>
<summary>Prompt implementasi Platform Configuration</summary>

```text
$implement-from-spec platform-configuration

Baca docs/specs/platform-configuration.md dan cek status modul di
docs/planning/progress.md. Mulai implementasi setelah spec dan cakupan pengerjaan
disetujui. Jika spec masih draf atau tiket masih menunggu persetujuan, laporkan prasyarat
yang tertahan.

Kerjakan cakupan yang disetujui dan jalankan kriteria penerimaannya, termasuk Bruno jika
ada API HTTP. Perbarui Kode selesai di Progres Modul setelah seluruh cakupan versi yang
dilacak terverifikasi.
```

</details>

### Return & Refund

- [ ] Spec selesai — [Draf spec](../specs/return-refund.md)
- [ ] Kode selesai

<details>
<summary>Prompt spec Return &amp; Refund</summary>

```text
$write-module-spec

Gunakan docs/specs/return-refund.md sebagai kerangka awal.
Bahas Return & Refund dengan merujuk status Order dan Payment. Mulai dari satu keputusan.
Dalam kondisi apa pembeli boleh mengajukan pengembalian barang?

Ajukan satu keputusan per putaran dengan rekomendasi dan contoh konkret. Tunggu jawaban
saya sebelum lanjut dan catat tambahan yang disepakati.

Rangkum cakupan dan kriteria penerimaan untuk saya konfirmasi. Setelah saya setuju,
lengkapi draf yang sama sesuai template skill, tautkan spec dari docs/README.md, dan
perbarui Progres Modul.
```

</details>

<details>
<summary>Prompt implementasi Return &amp; Refund</summary>

```text
$implement-from-spec return-refund

Baca docs/specs/return-refund.md dan cek status modul di docs/planning/progress.md. Mulai
implementasi setelah spec dan cakupan pengerjaan disetujui. Jika spec masih draf atau
tiket masih menunggu persetujuan, laporkan prasyarat yang tertahan.

Kerjakan cakupan yang disetujui dan jalankan kriteria penerimaannya, termasuk Bruno jika
ada API HTTP. Perbarui Kode selesai di Progres Modul setelah seluruh cakupan versi yang
dilacak terverifikasi.
```

</details>

### POS Management

- [ ] Spec selesai — [Draf spec](../specs/pos-management.md)
- [ ] Kode selesai

<details>
<summary>Prompt spec POS Management</summary>

```text
$write-module-spec

Gunakan docs/specs/pos-management.md sebagai kerangka awal.
Bahas POS Management dengan merujuk katalog dan Inventory per tenant. Mulai dari satu
keputusan. Apa yang harus bisa dilakukan kasir ketika koneksi terputus?

Ajukan satu keputusan per putaran dengan rekomendasi dan contoh konkret. Tunggu jawaban
saya sebelum lanjut dan catat tambahan yang disepakati.

Rangkum cakupan dan kriteria penerimaan untuk saya konfirmasi. Setelah saya setuju,
lengkapi draf yang sama sesuai template skill, tautkan spec dari docs/README.md, dan
perbarui Progres Modul.
```

</details>

<details>
<summary>Prompt implementasi POS Management</summary>

```text
$implement-from-spec pos-management

Baca docs/specs/pos-management.md dan cek status modul di docs/planning/progress.md. Mulai
implementasi setelah spec dan cakupan pengerjaan disetujui. Jika spec masih draf atau
tiket masih menunggu persetujuan, laporkan prasyarat yang tertahan.

Kerjakan cakupan yang disetujui dan jalankan kriteria penerimaannya, termasuk Bruno jika
ada API HTTP. Perbarui Kode selesai di Progres Modul setelah seluruh cakupan versi yang
dilacak terverifikasi.
```

</details>

### Review & Rating

- [ ] Spec selesai — [Draf spec](../specs/review-rating.md)
- [ ] Kode selesai

<details>
<summary>Prompt spec Review &amp; Rating</summary>

```text
$write-module-spec

Gunakan docs/specs/review-rating.md sebagai kerangka awal.
Bahas Review & Rating dengan merujuk identitas pembeli dan riwayat pesanan. Mulai dari
satu keputusan. Siapa yang boleh memberi ulasan pada produk?

Ajukan satu keputusan per putaran dengan rekomendasi dan contoh konkret. Tunggu jawaban
saya sebelum lanjut dan catat tambahan yang disepakati.

Rangkum cakupan dan kriteria penerimaan untuk saya konfirmasi. Setelah saya setuju,
lengkapi draf yang sama sesuai template skill, tautkan spec dari docs/README.md, dan
perbarui Progres Modul.
```

</details>

<details>
<summary>Prompt implementasi Review &amp; Rating</summary>

```text
$implement-from-spec review-rating

Baca docs/specs/review-rating.md dan cek status modul di docs/planning/progress.md. Mulai
implementasi setelah spec dan cakupan pengerjaan disetujui. Jika spec masih draf atau
tiket masih menunggu persetujuan, laporkan prasyarat yang tertahan.

Kerjakan cakupan yang disetujui dan jalankan kriteria penerimaannya, termasuk Bruno jika
ada API HTTP. Perbarui Kode selesai di Progres Modul setelah seluruh cakupan versi yang
dilacak terverifikasi.
```

</details>

### Audit & Activity Log

- [ ] Spec selesai — [Draf spec](../specs/audit-activity-log.md)
- [ ] Kode selesai

<details>
<summary>Prompt spec Audit &amp; Activity Log</summary>

```text
$write-module-spec

Gunakan docs/specs/audit-activity-log.md sebagai kerangka awal.
Bahas Audit & Activity Log dengan melihat audit yang sudah ada pada User Management. Mulai
dari satu keputusan. Peristiwa apa yang paling penting untuk dicatat pada versi pertama?

Ajukan satu keputusan per putaran dengan rekomendasi dan contoh konkret. Tunggu jawaban
saya sebelum lanjut dan catat tambahan yang disepakati.

Rangkum cakupan dan kriteria penerimaan untuk saya konfirmasi. Setelah saya setuju,
lengkapi draf yang sama sesuai template skill, tautkan spec dari docs/README.md, dan
perbarui Progres Modul.
```

</details>

<details>
<summary>Prompt implementasi Audit &amp; Activity Log</summary>

```text
$implement-from-spec audit-activity-log

Baca docs/specs/audit-activity-log.md dan cek status modul di docs/planning/progress.md.
Mulai implementasi setelah spec dan cakupan pengerjaan disetujui. Jika spec masih draf
atau tiket masih menunggu persetujuan, laporkan prasyarat yang tertahan.

Kerjakan cakupan yang disetujui dan jalankan kriteria penerimaannya, termasuk Bruno jika
ada API HTTP. Perbarui Kode selesai di Progres Modul setelah seluruh cakupan versi yang
dilacak terverifikasi.
```

</details>

### Subscription & Billing

- [ ] Spec selesai — [Draf spec](../specs/subscription-billing.md). Bersyarat pada keputusan monetisasi SaaS.
- [ ] Kode selesai

<details>
<summary>Prompt spec Subscription &amp; Billing</summary>

```text
$write-module-spec

Gunakan docs/specs/subscription-billing.md sebagai kerangka awal.
Bahas prasyarat Subscription & Billing. Mulai dari satu keputusan. Apakah Dagantara akan
memakai model langganan SaaS untuk tenant? Gunakan hasil keputusan itu sebagai dasar
cakupan billing.

Ajukan satu keputusan per putaran dengan rekomendasi dan contoh konkret. Tunggu jawaban
saya sebelum lanjut dan catat tambahan yang disepakati.

Rangkum cakupan dan kriteria penerimaan untuk saya konfirmasi. Setelah saya setuju,
lengkapi draf yang sama sesuai template skill, tautkan spec dari docs/README.md, dan
perbarui Progres Modul.
```

</details>

<details>
<summary>Prompt implementasi Subscription &amp; Billing</summary>

```text
$implement-from-spec subscription-billing

Baca docs/specs/subscription-billing.md dan cek status modul di docs/planning/progress.md.
Mulai implementasi setelah spec dan cakupan pengerjaan disetujui. Jika spec masih draf
atau tiket masih menunggu persetujuan, laporkan prasyarat yang tertahan.

Kerjakan cakupan yang disetujui dan jalankan kriteria penerimaannya, termasuk Bruno jika
ada API HTTP. Perbarui Kode selesai di Progres Modul setelah seluruh cakupan versi yang
dilacak terverifikasi.
```

</details>

## Frontend dan desain UI

Fondasi Angular dan Tailwind belum dimulai. Mulai fase ini dalam sesi baru setelah backend dalam cakupan yang disepakati selesai. Gunakan `$design-ui` untuk meninjau layout dan alur dengan spec backend, kontrak API, dan hasil penerimaannya sebagai konteks. Jika desain membutuhkan perilaku bisnis atau kontrak tambahan, catat keputusan itu pada spec modul terkait sebelum mengintegrasikannya.

<details>
<summary>Prompt mulai desain UI</summary>

```text
$design-ui

Baca bagian Frontend dan desain UI di docs/planning/progress.md. Gunakan spesifikasi,
kontrak API, dan hasil penerimaan yang tersedia untuk pekerjaan UI yang saya pilih.
Jika saya belum memilih, mulai dari 0C (Pola UI bersama). Gunakan spesifikasi User
Management untuk merancang satu form login dan satu tabel akun sebagai acuan bersama
Platform Admin dan Tenant Admin.

Buat wireframe atau prototipe dengan data contoh yang siap direview. Sertakan keadaan
memuat, kosong, dan gagal jika relevan. Jika prototipe interaktif, periksa penggunaan
keyboard dan fokus. Catat asumsi, keputusan yang perlu direview, dan cara mencoba hasilnya.
Jika desain membutuhkan perubahan izin, aturan bisnis, atau kontrak API, catat pertanyaan
dan rujuk spec atau tiket terkait sebelum integrasi.
```

</details>

### Tiga aplikasi frontend

| Aplikasi | Pengguna dan titik masuk | Layout |
| --- | --- | --- |
| **Platform Admin** | Operator dan admin Dagantara masuk melalui alamat Platform tersendiri. | Navigasi dan area kerja untuk tugas platform. |
| **Tenant Admin** | Pemilik dan staf masuk melalui `/admin` di alamat toko, misalnya `kopigayo.example/admin` atau `toko.dagantara.example/kopi-gayo/admin`. | Identitas toko, navigasi sesuai permission, dan area kerja pengelolaan toko. |
| **Storefront** | Pengunjung dan pembeli membuka domain toko atau alamat Dagantara dengan prefiks toko. | Kerangka publik dengan navigasi, area katalog, identitas toko, dan footer. |

Setiap aplikasi punya target build dan rilis sendiri. Pada MVP, satu versi Storefront melayani semua tenant dengan alamat, identitas, dan katalog masing-masing. Platform Admin dan Tenant Admin berbagi pola form, tabel, filter, dialog konfirmasi, dan pesan error. Rujuk [konteks login tenant](../specs/user-management.md#login-dan-konteks-tenant), [pemetaan Storefront](../specs/product-management.md#publikasi-dan-storefront), dan [rute Store Settings](../specs/store-settings.md#akses-dan-kepemilikan).

### Fondasi Angular dan Tailwind

| Urutan | Pekerjaan | Prasyarat dan peluang paralel | Bukti selesai |
| --- | --- | --- | --- |
| **0A** | Bootstrap Angular dan Tailwind | Mulai saat fase frontend dibuka. Susun konfigurasi proyek, Node, package manager, lockfile, dan koneksi API lokal. Bisa berjalan bersama 0B. | Dari checkout bersih, tiap aplikasi bisa dijalankan dan di-build secara mandiri. Catat perintah dev dan build serta konfigurasi API lokal. |
| **0B** | Rute dan akses | Rujuk User Management dan kontrak toko yang sudah ada. Kerjakan pemetaan host dan prefiks URL sambil 0A berjalan. | Platform punya titik masuk sendiri. URL toko memuat Storefront atau Tenant Admin sesuai jalur, termasuk saat tautan langsung dibuka atau halaman dimuat ulang. |
| **0C** | Pola UI bersama | Gunakan satu contoh form login dan satu tabel akun untuk menyepakati pola admin. Sketsa ketiga layout bisa berjalan bersamaan. | Form, tabel, filter, dan keadaan memuat, kosong, serta gagal bisa direview sebelum layar kedua aplikasi admin dirinci. |
| **1A** | Layout Platform Admin | Setelah rute utama dan pola admin disepakati, jalan paralel dengan 1B dan 1C. | Navigasi dan area konten Platform bisa diperiksa dengan contoh halaman daftar operator. |
| **1B** | Layout Tenant Admin | Setelah rute utama dan pola admin disepakati, jalan paralel dengan 1A dan 1C. | Nama toko dan navigasi sesuai permission bisa diperiksa dengan contoh halaman daftar staf. |
| **1C** | Layout Storefront | Setelah konteks toko dan rute publik jelas, jalan paralel dengan layout admin. | Kerangka katalog, identitas toko, dan footer bisa diperiksa pada layar kecil. Rujuk spec Tenant Management untuk keadaan toko tidak tersedia. |

<details>
<summary>Prompt fondasi Angular dan Tailwind (0A)</summary>

```text
Kerjakan 0A (Bootstrap Angular dan Tailwind) di bagian Frontend dan desain UI pada
docs/planning/progress.md. Baca AGENTS.md dan docs/README.md, lalu periksa struktur repo
dan konfigurasi frontend yang sudah ada.

Siapkan fondasi untuk Platform Admin, Tenant Admin, dan Storefront. Pastikan setiap
aplikasi memakai Tailwind dan punya target dev serta build sendiri. Ikuti konvensi repo
untuk versi Node, package manager, dan lockfile. Jika belum ada, pilih versi yang saling
kompatibel dan catat pilihannya. Sediakan konfigurasi koneksi ke API lokal dengan merujuk
backend/README.md.

Verifikasi instalasi dari lockfile di direktori bersih. Jalankan dev server dan build
setiap aplikasi secara mandiri. Periksa bahwa styling Tailwind tampil, lalu uji koneksi
API lokal jika backend tersedia. Catat versi Node, package manager, cara instalasi,
perintah dev dan build, serta konfigurasi API lokal. Laporkan hasil per aplikasi dan
pemeriksaan yang masih tertahan. Gunakan 0B dan seterusnya sebagai rujukan pekerjaan lanjutan.
```

</details>

### Alur pertama yang diverifikasi di browser

| Aplikasi | Alur pertama | Hasil yang diperiksa |
| --- | --- | --- |
| **Platform Admin** | Login → buka daftar operator → buka detail operator. | Akun Platform masuk dari alamat yang benar dan dapat melihat daftar serta detail sesuai permission. |
| **Tenant Admin** | Buka `/admin` di URL toko → masuk dengan password sementara → ganti password → buka daftar staf. | Akses operasional tersedia setelah ganti password. Sesi satu toko ditolak saat dipakai pada alamat toko lain. |
| **Storefront** | Buka URL toko → buka daftar produk → buka detail → pilih varian. | Pengunjung melihat produk terbit milik toko dan status stok varian yang dipilih. |

Dua alur admin bisa dibangun paralel setelah contoh form login dan tabel direview. Alur Storefront bisa berjalan bersamaan setelah rute publik dan layoutnya siap. Gunakan kontrak [User Management](../specs/user-management.md), [Product Management](../specs/product-management.md#publikasi-dan-storefront), dan [Inventory Management](../specs/inventory-management.md#ketersediaan-untuk-storefront) untuk ketiga alur tersebut.

### Urutan sesi desain lintas modul

Urutan ini adalah urutan review. Mulai pekerjaan yang prasyaratnya sudah siap pada fase frontend. Tunda integrasi layar yang masih menunggu kontrak spesifik, sambil melanjutkan bagian yang bisa direview dengan data contoh.

| Urutan | Sesi dan modul rujukan | Dapat dikerjakan paralel | Prasyarat dan hasil review |
| --- | --- | --- | --- |
| **1** | **Login Platform dan Tenant Admin** — [User Management](../specs/user-management.md#login-dan-konteks-tenant) | Rinci kedua alur login setelah pola form dan konteks akses jelas. Review awal Storefront bisa berjalan bersamaan. | Verifikasi titik masuk, ganti password sementara, dan keadaan ketika akses ditolak pada alur pertama tiap aplikasi admin. |
| **2** | **Kelola akun dan role** — [User Management](../specs/user-management.md#role-permission-dan-akun-yang-dilindungi) | Akun operator, role Platform, dan Help Center bisa maju bersama akun staf dan role Tenant Admin. | Gunakan pola daftar, detail, dan aksi akun yang sama. Periksa bahwa tiap aplikasi memperlihatkan tindakan sesuai izin pengguna. |
| **3** | **Katalog dan stok di Tenant Admin** — [Product](../specs/product-management.md#opsi-varian-dan-sku) + [Inventory](../specs/inventory-management.md#penerimaan-barang) | Setelah pola pemilihan produk, varian, dan SKU jelas, kategori, media, dan publikasi bisa maju bersama penerimaan, koreksi, dan saldo stok. Sesi ini bisa maju sambil jalur akun dirinci. | Perlu konteks akses Tenant Admin dan kontrak varian yang stabil. Review dari memilih SKU sampai saldo dan riwayatnya terlihat. |
| **4** | **Identitas toko dan katalog publik** — [Store Settings](../specs/store-settings.md#isian-penyimpanan-dan-tampilan) + [Storefront](#storefront-management) + [Product](../specs/product-management.md#publikasi-dan-storefront) + [Inventory](../specs/inventory-management.md#ketersediaan-untuk-storefront) + [Discovery](../specs/product-discovery.md#perilaku-tahap-frontend) | Form identitas toko dan kerangka katalog publik bisa maju bersamaan setelah data identitas jelas. Detail produk dapat direview memakai kontrak publik yang sudah ada. | Review perubahan identitas dari admin ke toko, pencarian, pemilihan kategori, detail dan stok varian. Integrasi Discovery menunggu API dan kontrak sumber pilihan kategori. Verifikasi `PD-AT-07` sampai `PD-AT-09` dengan browser. |
| **5** | **Alur beli di Storefront** — [Customer](../specs/customer-management.md) + [Cart](../specs/cart-management.md) + [Checkout](../specs/checkout-management.md) + [Address & Location](../specs/address-location-management.md) + [Order](../specs/order-management.md) | Rinci bagian Cart dan alamat setelah identitas pembeli serta data wajib jelas. Satukan keduanya saat review Checkout. | Tunggu keputusan tamu atau login, identitas keranjang, data wajib, serta pilihan bayar dan kirim. Review dari keranjang sampai konfirmasi pesanan. |
| **6** | **Pesanan setelah checkout** — [Order](../specs/order-management.md) + [Payment](../specs/payment-management.md) + [Shipping](../specs/shipping-management.md) | Desain bukti transfer dan verifikasi bisa maju bersama desain pemenuhan dan pelacakan setelah kontrak Order dan tindakan tiap aktor jelas. | Review halaman pesanan pembeli bersama tindakan admin. Periksa bahwa hasil verifikasi pembayaran dan pengiriman terlihat sesuai status yang disepakati. |

### Storefront Management

- [ ] Spec selesai — [Draf spec](../specs/storefront-management.md) untuk navigasi dan susunan storefront dasar.
- [ ] Desain UI selesai — [daftar dan detail publik dasar](../tickets/product-management/03-publikasi-dan-storefront.md) tersedia sebagai acuan.
- [ ] Implementasi frontend selesai.

<details>
<summary>Prompt spec Storefront Management</summary>

```text
$write-module-spec

Gunakan docs/specs/storefront-management.md sebagai kerangka awal.
Bahas Storefront Management versi dasar dengan melihat daftar dan detail publik yang sudah
ada. Mulai dari satu keputusan. Navigasi apa yang perlu tersedia bagi pengunjung saat
membuka toko? Rujuk Store Settings untuk identitas toko dan Product Discovery untuk
pencarian.

Ajukan satu keputusan per putaran dengan rekomendasi dan contoh konkret. Tunggu jawaban
saya sebelum lanjut dan catat tambahan yang disepakati.

Rangkum cakupan dan kriteria penerimaan untuk saya konfirmasi. Setelah saya setuju,
lengkapi draf yang sama sesuai template skill, tautkan spec dari docs/README.md, dan
perbarui Progres Modul.
```

</details>

<details>
<summary>Prompt implementasi Storefront Management</summary>

```text
$implement-from-spec storefront-management

Baca docs/specs/storefront-management.md dan sesi katalog publik di
docs/planning/progress.md. Mulai setelah spec dan cakupan implementasi disetujui.
Gunakan layout Storefront dan kontrak backend yang dirujuk oleh sesi tersebut.

Jalankan kriteria penerimaan yang berlaku, termasuk Bruno untuk API HTTP dalam cakupan.
Perbarui status Implementasi frontend selesai setelah cakupan terverifikasi di browser.
```

</details>
