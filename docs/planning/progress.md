# Progres Modul Dagantara

Centang **Spec selesai** setelah spesifikasi versi yang disepakati disetujui dan ditulis. Centang **Kode selesai** setelah implementasi dalam cakupan itu selesai dan verifikasi penerimaan lulus. Tautkan spec dan bukti penyelesaian dari entri modul. Perbarui penanda **Berikutnya dibahas** saat giliran berpindah.

Lihat [roadmap](module-roadmap.md#module-matrix) untuk cakupan dan prioritas modul serta [daftar dependensi](development-dependencies.md#open-decisions--blockers) untuk keputusan yang masih terbuka.

Setelah memperbarui checklist, ikuti [langkah regenerasi viewer](../README.md#regenerasi-viewer) agar salinan yang tertanam ikut diperbarui.

Buka **Prompt spec** pada modul yang menunggu pembahasan untuk menyalin prompt awal. Setiap prompt merujuk ke **Draf spec** pada entri yang sama. Tambahkan atau perjelas detail sesuai hasil pembahasan. Buka **Prompt implementasi** setelah spec dan cakupan pengerjaan disetujui.

## MVP

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

- [ ] Spec selesai — [Draf spec](../specs/tenant-management.md). Onboarding dan pemetaan URL sudah dicakup oleh [User Management](../specs/user-management.md#pengajuan-dan-provisioning-tenant). Siklus hidup tenant dan domain masih perlu dibahas.
- [ ] Kode selesai

<details>
<summary>Prompt spec Tenant Management</summary>

```text
$write-module-spec

Gunakan docs/specs/tenant-management.md sebagai kerangka awal.
Bahas Tenant Management dari DEP-01 dan alur onboarding di docs/specs/user-management.md.
Mulai dari satu keputusan. Saat tenant dinonaktifkan, apa yang harus terjadi pada URL toko
publiknya?

Ajukan satu keputusan per putaran dengan rekomendasi dan contoh konkret. Tunggu jawaban
saya sebelum lanjut dan catat tambahan yang disepakati.

Rangkum cakupan dan kriteria penerimaan untuk saya konfirmasi. Setelah saya setuju,
lengkapi draf yang sama sesuai template skill, tautkan spec dari docs/README.md, dan
perbarui Progres Modul.
```

</details>

<details>
<summary>Prompt implementasi Tenant Management</summary>

```text
$implement-from-spec tenant-management

Baca docs/specs/tenant-management.md dan cek status modul di docs/planning/progress.md.
Mulai implementasi setelah spec dan cakupan pengerjaan disetujui. Jika spec masih draf
atau tiket masih menunggu persetujuan, laporkan prasyarat yang tertahan.

Kerjakan cakupan yang disetujui dan jalankan kriteria penerimaannya, termasuk Bruno jika
ada API HTTP. Perbarui Kode selesai di Progres Modul setelah seluruh cakupan versi yang
dilacak terverifikasi.
```

</details>

### Store Settings

- [x] Spec selesai — [Identitas Publik Toko](../specs/store-settings.md)
- [ ] Kode selesai — [dua tiket implementasi](../specs/store-settings.md#implementation-tickets)

<details>
<summary>Prompt Store Settings</summary>

```text
$write-module-spec

Lanjutkan Store Settings dari docs/specs/store-settings.md. Tinjau usulan tiket 01 dan 02. Bantu saya menentukan apakah pembagian itu sudah bisa disetujui, lalu tunggu jawaban saya sebelum menulis tiket.
```

</details>

<details>
<summary>Prompt implementasi Store Settings</summary>

```text
$implement-from-spec store-settings

Baca docs/specs/store-settings.md dan cek status modul di docs/planning/progress.md. Mulai
implementasi setelah spec dan cakupan pengerjaan disetujui. Jika pembagian tiket 01 dan 02
masih menunggu persetujuan, laporkan statusnya dan tunggu persetujuan.

Kerjakan cakupan yang disetujui dan jalankan kriteria penerimaannya, termasuk Bruno jika
ada API HTTP. Perbarui Kode selesai di Progres Modul setelah seluruh cakupan versi yang
dilacak terverifikasi.
```

</details>

### Storefront Management

- [ ] Spec selesai — [Draf spec](../specs/storefront-management.md)
- [ ] Kode selesai — [daftar dan detail publik dasar](../tickets/product-management/03-publikasi-dan-storefront.md) sudah tersedia.

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

Baca docs/specs/storefront-management.md dan cek status modul di
docs/planning/progress.md. Mulai implementasi setelah spec dan cakupan pengerjaan
disetujui. Jika spec masih draf atau tiket masih menunggu persetujuan, laporkan prasyarat
yang tertahan.

Kerjakan cakupan yang disetujui dan jalankan kriteria penerimaannya, termasuk Bruno jika
ada API HTTP. Perbarui Kode selesai di Progres Modul setelah seluruh cakupan versi yang
dilacak terverifikasi.
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
- [ ] Kode selesai — API dan verifikasi Bruno. [Frontend dan uji browser](../specs/product-discovery.md#perilaku-tahap-frontend) menyusul.

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

## Pascaversi MVP

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
Bahas Tax Management dari DEP-07. Mulai dari satu keputusan desain. Pada tahap mana
pembeli perlu melihat rincian pajak pada alur checkout?

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
