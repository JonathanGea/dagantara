# Store Settings 01 — Nama dan kontak publik

Spec induk: [Store Settings](../../specs/store-settings.md)

Status: ready-for-agent

## What to Build

Implementasikan bagian nama tampilan dan kontak publik dari SS-01, SS-03, SS-04, dan SS-05. Sediakan form `/admin/store-settings`, permission `store_settings.manage`, pengaturan per tenant, dan API admin `GET` serta `PUT /api/v1/store-settings`. Gunakan kontrak multipart `settings` dan aksi `KEEP` untuk penyimpanan teks pada tahap ini. Tampilkan identitas dan kontak pada daftar dan detail produk. Penanganan file dan aksi logo masuk tiket 02.

## Acceptance Criteria

- [ ] SS-AT-01 — Admin tenant lama dapat membuka form dan API mengembalikan nama provisioning serta nilai opsional `null`. Daftar kosong, daftar berisi produk terbit, dan detail produk memakai nama provisioning tanpa bagian kontak.
- [ ] SS-AT-02 — Nama tampilan dan kedua tautan kontak tampil setelah disimpan dan dibaca ulang. Mengosongkan nama dan email mengembalikan nama provisioning dan menyisakan WhatsApp. Mengosongkan WhatsApp menyembunyikan bagian kontak pada daftar dan detail.
- [ ] Bagian teks SS-AT-04 — Input kontak yang tidak valid menghasilkan `400`. Pembacaan ulang admin dan storefront memperlihatkan seluruh nilai lama, dan form menampilkan pesan kegagalan.
- [ ] SS-AT-05 — Permintaan baca dan simpan tanpa sesi menghasilkan `401`. Akun tanpa izin, akun platform, dan sesi admin A pada URL B mendapat `403`. Pengaturan kedua tenant tidak berubah. Tenant baru memperoleh permission untuk role `TENANT_ADMIN` dan delegasi mengikuti aturan role tenant.
- [ ] Bagian identitas dan produk SS-AT-06 — Hostname langsung dan URL berprefiks menampilkan nama, kontak, dan produk tenant yang terpetakan. Perubahan pada A tidak muncul pada B.
- [ ] Verifikasi kontrak HTTP melalui Bruno dengan `bru run --env Local` dan periksa form, daftar, serta detail di browser. Catat perintah dan hasil per skenario.

## Blocked By

Tiket dapat dimulai dengan pemetaan URL dan login dari [User Management](../../specs/user-management.md#login-dan-konteks-tenant) serta endpoint dan halaman dari [Product Management](../../specs/product-management.md#publikasi-dan-storefront).
