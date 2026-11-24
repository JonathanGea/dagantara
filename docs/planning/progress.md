# Progres Modul Dagantara

Centang **Spec selesai** setelah spesifikasi versi yang disepakati disetujui dan ditulis. Centang **Kode selesai** setelah implementasi dalam cakupan itu selesai dan verifikasi penerimaan lulus. Tautkan spec dan bukti penyelesaian dari entri modul. Perbarui penanda **Berikutnya dibahas** saat giliran berpindah.

Lihat [roadmap](module-roadmap.md#module-matrix) untuk cakupan dan prioritas modul serta [daftar dependensi](development-dependencies.md#open-decisions--blockers) untuk keputusan yang masih terbuka.

Setelah memperbarui checklist, ikuti [langkah regenerasi viewer](../README.md#regenerasi-viewer) agar salinan yang tertanam ikut diperbarui.

## MVP

### User Management

- [x] Spec selesai — [Akses Platform dan Tenant](../specs/user-management.md)
- [x] Kode selesai — [lima tiket implementasi](../specs/user-management.md#implementation-tickets)

### Product Management

- [x] Spec selesai — [Product Management](../specs/product-management.md)
- [x] Kode selesai — [tiga tiket implementasi](../specs/product-management.md#implementation-tickets)

### Tenant Management

- [ ] Spec selesai — onboarding dan pemetaan URL sudah dicakup oleh [User Management](../specs/user-management.md#pengajuan-dan-provisioning-tenant). Siklus hidup tenant dan domain masih perlu dibahas.
- [ ] Kode selesai

### Store Settings

- [x] Spec selesai — [Identitas Publik Toko](../specs/store-settings.md)
- [ ] Kode selesai — [pembagian tiket menunggu persetujuan](../specs/store-settings.md#implementation-tickets)

### Storefront Management

- [ ] Spec selesai
- [ ] Kode selesai — [daftar dan detail publik dasar](../tickets/product-management/03-publikasi-dan-storefront.md) sudah tersedia.

### Inventory Management

- [x] Spec selesai — [Inventory Management](../specs/inventory-management.md)
- [x] Kode selesai — [dua tiket implementasi](../specs/inventory-management.md#implementation-tickets)

### Product Catalog / Discovery

- [x] Spec selesai — [Pencarian dan Filter Produk Publik](../specs/product-discovery.md)
- [ ] Kode selesai — API dan verifikasi Bruno. [Frontend dan uji browser](../specs/product-discovery.md#perilaku-tahap-frontend) menyusul.

### Customer Management

**Berikutnya dibahas.** Apakah pembeli bisa checkout sebagai tamu atau perlu login? [DEP-04](development-dependencies.md#open-decisions--blockers)

- [ ] Spec selesai
- [ ] Kode selesai

### Cart Management

- [ ] Spec selesai
- [ ] Kode selesai

### Checkout Management

- [ ] Spec selesai
- [ ] Kode selesai

### Order Management

- [ ] Spec selesai
- [ ] Kode selesai

### Payment Management

- [ ] Spec selesai
- [ ] Kode selesai

### Shipping Management

- [ ] Spec selesai
- [ ] Kode selesai

### Address & Location Management

- [ ] Spec selesai
- [ ] Kode selesai

## Pascaversi MVP

### Pricing & Promotion

- [ ] Spec selesai
- [ ] Kode selesai

### Notification Management

- [ ] Spec selesai
- [ ] Kode selesai

### Reporting & Analytics

- [ ] Spec selesai
- [ ] Kode selesai

### Tax Management

- [ ] Spec selesai
- [ ] Kode selesai

### Platform Configuration

- [ ] Spec selesai
- [ ] Kode selesai

### Return & Refund

- [ ] Spec selesai
- [ ] Kode selesai

### POS Management

- [ ] Spec selesai
- [ ] Kode selesai

### Review & Rating

- [ ] Spec selesai
- [ ] Kode selesai

### Audit & Activity Log

- [ ] Spec selesai
- [ ] Kode selesai

### Subscription & Billing

- [ ] Spec selesai — bersyarat pada keputusan monetisasi SaaS.
- [ ] Kode selesai
