# Progres Modul Dagantara

Centang **Spec selesai** setelah spesifikasi versi yang disepakati disetujui dan ditulis. Centang **Kode selesai** setelah implementasi dalam cakupan itu selesai dan verifikasi penerimaan lulus. Tautkan spec dan bukti penyelesaian dari entri modul. Perbarui penanda **Berikutnya dibahas** saat giliran berpindah.

Lihat [roadmap](module-roadmap.md#module-matrix) untuk cakupan dan prioritas modul serta [daftar dependensi](development-dependencies.md#open-decisions--blockers) untuk keputusan yang masih terbuka.

Setelah memperbarui checklist, ikuti [langkah regenerasi viewer](../README.md#regenerasi-viewer) agar salinan yang tertanam ikut diperbarui.

Buka **Draf spec** untuk melihat kerangka awal. Buka **Prompt** di entri modul untuk menyalin prompt awal. Tambahkan atau perjelas detail sesuai hasil pembahasan.

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
<summary>Prompt Tenant Management</summary>

```text
$write-module-spec

Bahas Tenant Management dari DEP-01 dan alur onboarding di docs/specs/user-management.md. Mulai dari satu keputusan. Saat tenant dinonaktifkan, apa yang harus terjadi pada URL toko publiknya?
```

</details>

### Store Settings

- [x] Spec selesai — [Identitas Publik Toko](../specs/store-settings.md)
- [ ] Kode selesai — [pembagian tiket menunggu persetujuan](../specs/store-settings.md#implementation-tickets)

<details>
<summary>Prompt Store Settings</summary>

```text
$write-module-spec

Lanjutkan Store Settings dari docs/specs/store-settings.md. Tinjau usulan tiket 01 dan 02. Bantu saya menentukan apakah pembagian itu sudah bisa disetujui, lalu tunggu jawaban saya sebelum menulis tiket.
```

</details>

### Storefront Management

- [ ] Spec selesai — [Draf spec](../specs/storefront-management.md)
- [ ] Kode selesai — [daftar dan detail publik dasar](../tickets/product-management/03-publikasi-dan-storefront.md) sudah tersedia.

<details>
<summary>Prompt Storefront Management</summary>

```text
$write-module-spec

Bahas Storefront Management versi dasar dengan melihat daftar dan detail publik yang sudah ada. Mulai dari satu keputusan. Navigasi apa yang perlu tersedia bagi pengunjung saat membuka toko? Rujuk Store Settings untuk identitas toko dan Product Discovery untuk pencarian.
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
<summary>Prompt Product Catalog / Discovery</summary>

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
<summary>Prompt Customer Management</summary>

```text
$write-module-spec

Bahas Customer Management dari DEP-04. Jika pengunjung baru ingin membeli Kopi Arabika Gayo, bolehkah dia checkout sebagai tamu atau perlu login? Beri rekomendasi untuk versi pertama, contoh konkret, dan konsekuensinya bagi keranjang dan checkout. Tunggu jawaban saya.
```

</details>

### Cart Management

- [ ] Spec selesai — [Draf spec](../specs/cart-management.md)
- [ ] Kode selesai

<details>
<summary>Prompt Cart Management</summary>

```text
$write-module-spec

Bahas Cart Management dengan merujuk keputusan Customer Management dan DEP-02. Mulai dari satu keputusan. Bagaimana toko mengenali keranjang pembeli ketika ia kembali membuka URL toko?
```

</details>

### Checkout Management

- [ ] Spec selesai — [Draf spec](../specs/checkout-management.md)
- [ ] Kode selesai

<details>
<summary>Prompt Checkout Management</summary>

```text
$write-module-spec

Bahas Checkout Management dengan merujuk keputusan Customer, Cart, Payment, dan Shipping yang sudah tersedia. Mulai dari satu keputusan. Data apa yang wajib diisi pembeli sebelum bisa membuat pesanan?
```

</details>

### Order Management

- [ ] Spec selesai — [Draf spec](../specs/order-management.md)
- [ ] Kode selesai

<details>
<summary>Prompt Order Management</summary>

```text
$write-module-spec

Bahas Order Management dari DEP-06 dan alur Checkout. Mulai dari satu keputusan. Apa status awal pesanan ketika pembeli berhasil checkout?
```

</details>

### Payment Management

- [ ] Spec selesai — [Draf spec](../specs/payment-management.md)
- [ ] Kode selesai

<details>
<summary>Prompt Payment Management</summary>

```text
$write-module-spec

Bahas Payment Management versi transfer bank manual dengan merujuk status Order. Mulai dari satu keputusan. Bukti apa yang perlu diberikan pembeli agar admin bisa memverifikasi pembayaran?
```

</details>

### Shipping Management

- [ ] Spec selesai — [Draf spec](../specs/shipping-management.md)
- [ ] Kode selesai

<details>
<summary>Prompt Shipping Management</summary>

```text
$write-module-spec

Bahas Shipping Management dari DEP-05. Mulai dari satu keputusan. Bagaimana toko menghitung ongkir yang ditampilkan sebelum pembeli membuat pesanan?
```

</details>

### Address & Location Management

- [ ] Spec selesai — [Draf spec](../specs/address-location-management.md)
- [ ] Kode selesai

<details>
<summary>Prompt Address &amp; Location Management</summary>

```text
$write-module-spec

Bahas Address & Location Management dengan merujuk kebutuhan Customer dan Shipping. Mulai dari satu keputusan. Data alamat tujuan minimum apa yang diperlukan agar pesanan bisa dikirim?
```

</details>

## Pascaversi MVP

### Pricing & Promotion

- [ ] Spec selesai — [Draf spec](../specs/pricing-promotion.md)
- [ ] Kode selesai

<details>
<summary>Prompt Pricing &amp; Promotion</summary>

```text
$write-module-spec

Bahas Pricing & Promotion dengan merujuk DEP-07 dan kontrak harga pada pesanan. Mulai dari satu keputusan. Jenis promosi apa yang perlu didukung pada versi pertama?
```

</details>

### Notification Management

- [ ] Spec selesai — [Draf spec](../specs/notification-management.md)
- [ ] Kode selesai

<details>
<summary>Prompt Notification Management</summary>

```text
$write-module-spec

Bahas Notification Management dengan merujuk alur Order dan Payment. Mulai dari satu keputusan. Setelah pesanan dibuat, pemberitahuan apa yang paling perlu diterima pembeli?
```

</details>

### Reporting & Analytics

- [ ] Spec selesai — [Draf spec](../specs/reporting-analytics.md)
- [ ] Kode selesai

<details>
<summary>Prompt Reporting &amp; Analytics</summary>

```text
$write-module-spec

Bahas Reporting & Analytics dengan melihat data pesanan, pembayaran, dan stok yang tersedia per tenant. Mulai dari satu keputusan. Keputusan apa yang ingin dibantu oleh laporan pertama untuk pengelola toko?
```

</details>

### Tax Management

- [ ] Spec selesai — [Draf spec](../specs/tax-management.md)
- [ ] Kode selesai

<details>
<summary>Prompt Tax Management</summary>

```text
$write-module-spec

Bahas Tax Management dari DEP-07. Mulai dari satu keputusan desain. Pada tahap mana pembeli perlu melihat rincian pajak pada alur checkout?
```

</details>

### Platform Configuration

- [ ] Spec selesai — [Draf spec](../specs/platform-configuration.md)
- [ ] Kode selesai

<details>
<summary>Prompt Platform Configuration</summary>

```text
$write-module-spec

Bahas Platform Configuration dengan merujuk aturan akun dan izin platform. Mulai dari satu keputusan. Pengaturan global apa yang pertama kali perlu bisa diubah oleh operator platform?
```

</details>

### Return & Refund

- [ ] Spec selesai — [Draf spec](../specs/return-refund.md)
- [ ] Kode selesai

<details>
<summary>Prompt Return &amp; Refund</summary>

```text
$write-module-spec

Bahas Return & Refund dengan merujuk status Order dan Payment. Mulai dari satu keputusan. Dalam kondisi apa pembeli boleh mengajukan pengembalian barang?
```

</details>

### POS Management

- [ ] Spec selesai — [Draf spec](../specs/pos-management.md)
- [ ] Kode selesai

<details>
<summary>Prompt POS Management</summary>

```text
$write-module-spec

Bahas POS Management dengan merujuk katalog dan Inventory per tenant. Mulai dari satu keputusan. Apa yang harus bisa dilakukan kasir ketika koneksi terputus?
```

</details>

### Review & Rating

- [ ] Spec selesai — [Draf spec](../specs/review-rating.md)
- [ ] Kode selesai

<details>
<summary>Prompt Review &amp; Rating</summary>

```text
$write-module-spec

Bahas Review & Rating dengan merujuk identitas pembeli dan riwayat pesanan. Mulai dari satu keputusan. Siapa yang boleh memberi ulasan pada produk?
```

</details>

### Audit & Activity Log

- [ ] Spec selesai — [Draf spec](../specs/audit-activity-log.md)
- [ ] Kode selesai

<details>
<summary>Prompt Audit &amp; Activity Log</summary>

```text
$write-module-spec

Bahas Audit & Activity Log dengan melihat audit yang sudah ada pada User Management. Mulai dari satu keputusan. Peristiwa apa yang paling penting untuk dicatat pada versi pertama?
```

</details>

### Subscription & Billing

- [ ] Spec selesai — [Draf spec](../specs/subscription-billing.md). Bersyarat pada keputusan monetisasi SaaS.
- [ ] Kode selesai

<details>
<summary>Prompt Subscription &amp; Billing</summary>

```text
$write-module-spec

Bahas prasyarat Subscription & Billing. Mulai dari satu keputusan. Apakah Dagantara akan memakai model langganan SaaS untuk tenant? Gunakan hasil keputusan itu sebagai dasar cakupan billing.
```

</details>
