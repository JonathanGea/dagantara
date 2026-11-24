# Store Settings 01 — Nama dan kontak publik

Spec induk: [Store Settings](../../specs/store-settings.md)

Status: done

## What to Build

Implementasikan bagian nama tampilan dan kontak publik dari SS-01, SS-03, SS-04, dan SS-05. Sediakan form `/admin/store-settings`, permission `store_settings.manage`, pengaturan per tenant, dan API admin `GET` serta `PUT /api/v1/store-settings`. Gunakan kontrak multipart `settings` dan aksi `KEEP` untuk penyimpanan teks pada tahap ini. Tampilkan identitas dan kontak pada daftar dan detail produk. Penanganan file dan aksi logo masuk tiket 02.

## Acceptance Criteria

- [x] SS-AT-01 — Admin tenant lama dapat membuka form dan API mengembalikan nama provisioning serta nilai opsional `null`. Daftar kosong, daftar berisi produk terbit, dan detail produk memakai nama provisioning tanpa bagian kontak.
- [x] SS-AT-02 — Nama tampilan dan kedua tautan kontak tampil setelah disimpan dan dibaca ulang. Mengosongkan nama dan email mengembalikan nama provisioning dan menyisakan WhatsApp. Mengosongkan WhatsApp menyembunyikan bagian kontak pada daftar dan detail.
- [x] Bagian teks SS-AT-04 — Input kontak yang tidak valid menghasilkan `400`. Pembacaan ulang admin dan storefront memperlihatkan seluruh nilai lama, dan form menampilkan pesan kegagalan.
- [x] SS-AT-05 — Permintaan baca dan simpan tanpa sesi menghasilkan `401`. Akun tanpa izin, akun platform, dan sesi admin A pada URL B mendapat `403`. Pengaturan kedua tenant tidak berubah. Tenant baru memperoleh permission untuk role `TENANT_ADMIN` dan delegasi mengikuti aturan role tenant.
- [x] Bagian identitas dan produk SS-AT-06 — Hostname langsung dan URL berprefiks menampilkan nama, kontak, dan produk tenant yang terpetakan. Perubahan pada A tidak muncul pada B.
- [x] Verifikasi kontrak HTTP melalui Bruno dengan `bru run --env Local` dan periksa form, daftar, serta detail di browser. Catat perintah dan hasil per skenario.

## Bukti verifikasi

Siapkan PostgreSQL terisolasi dengan migrasi 01–09 melalui `psql -X -v ON_ERROR_STOP=1 -f <file>`. Ekspor `SS01_FIXTURE_PASSWORD`, terapkan fixture, lalu jalankan `10-store-settings.sql`. Urutan ini memastikan kedua role `TENANT_ADMIN` sudah ada sebelum migrasi 10. Pengujian memakai PostgreSQL 16 di port `15439` dan backend JDK 25 di port `18089`. Backend dijalankan dengan `APP_PLATFORM_HOST=localhost` melalui `run-backend.ps1 -Port 18089 -Foreground`. Pemeriksaan `/actuator/health` mengembalikan `UP`. [Fixture Bruno](../../../bruno/store-settings/fixture.sql)

Dari `backend/`, jalankan `.\mvnw.cmd -B -ntp verify` dengan `STORE_SETTINGS01_TEST_DB_*`, `PRODUCT01_TEST_DB_*`, `PRODUCT03_TEST_DB_*`, dan `INVENTORY01_TEST_DB_*` berisi `URL`, `USERNAME`, dan `PASSWORD` PostgreSQL uji. Hasilnya `BUILD SUCCESS`, 29 tes lulus tanpa skip, termasuk 4 tes Store Settings. Suite Store Settings dan Storefront menjalankan Spring pada port acak dalam schema terpisah.

Dari `bruno/`, gunakan Bruno CLI 4.3.0 dengan `SS01_FIXTURE_PASSWORD` yang sama dengan fixture.

```powershell
bru run smoke --env Local --env-var baseUrl=http://localhost:18089 --bail --noproxy
bru run store-settings -r --env Local --env-var baseUrl=http://localhost:18089 --bail --noproxy --sandbox developer
```

Smoke lulus 2/2 tes. Kelima skenario Store Settings lulus 5/5 tes. Browser Orca memakai `orca tab create --url <url>`, `orca goto --page <id> --url <url>`, dan `orca snapshot --page <id> --urls`. Form dijalankan melalui `orca fill`, `orca clear`, dan `orca click` pada halaman Store Settings.

| Kriteria | Hasil |
| --- | --- |
| SS-AT-01 | Bruno menerima `200` dengan nama provisioning dan keempat isian opsional `null`. Tes Spring memastikan migrasi memberi izin ke role lama dan pembacaan awal tidak membuat record. Browser menampilkan `Kopi Pagi Nusantara` sebagai petunjuk form serta sebagai nama toko pada daftar kosong, daftar terbit, dan detail. Bagian kontak tersembunyi. |
| SS-AT-02 | Bruno membuktikan trimming, baca ulang admin, dan hasil pada HTML daftar serta detail. Browser menampilkan `Kopi Pagi` dan tautan `https://wa.me/6281234567890` serta `mailto:kopi@example.test`. Mengosongkan nama dan email menampilkan nama provisioning dengan WhatsApp saja. Mengosongkan WhatsApp menyembunyikan kontak di kedua halaman. |
| SS-AT-04 (teks) | Bruno menerima `400` saat nomor WhatsApp atau email tidak valid. Spring menguji batas 255 karakter, bagian multipart wajib, dan penolakan file bersama `KEEP`. Browser menampilkan pesan kegagalan saat email tidak valid. Login ulang memuat ketiga nilai yang terakhir berhasil disimpan dan nama serta kedua tautannya masih tampil di daftar dan detail. |
| SS-AT-05 | Bruno membuktikan GET/PUT tanpa sesi mengembalikan `401`, sedangkan akun tanpa izin, akun platform, dan sesi A pada URL B mengembalikan `403`. Pembacaan ulang membuktikan data kedua tenant tetap sama. Provisioning memberi izin langsung kepada `TENANT_ADMIN`. Delegasi dari admin berhasil dan upaya menambah izin sendiri oleh akun tanpa izin ditolak. |
| SS-AT-06 (identitas dan produk) | Bruno memeriksa daftar produk dan penolakan detail lintas tenant. Di browser, `127.0.0.1:18089` menampilkan identitas dan produk Kopi Pagi, sedangkan `localhost:18089/ss01-b` menampilkan identitas dan produk Teh Sore. Setelah A mengosongkan pengaturannya, daftar dan detail B tetap menampilkan nama, dua kontak, dan produk B. |

## Blocked By

Tiket dapat dimulai dengan pemetaan URL dan login dari [User Management](../../specs/user-management.md#login-dan-konteks-tenant) serta endpoint dan halaman dari [Product Management](../../specs/product-management.md#publikasi-dan-storefront).
