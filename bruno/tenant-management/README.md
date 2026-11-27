# Penerimaan Tenant Management v1

Skenario ini mengikuti [TM-AT-01 sampai TM-AT-05](../../docs/specs/tenant-management.md#testing-decisions).

## Menyiapkan fixture

1. Gunakan database PostgreSQL kosong yang dapat dibuang dan terapkan migrasi 01–11. Ekspor `DAGANTARA_TM_TEST_PASSWORD` dari penyimpanan rahasia lokal. Fixture membuat akun Admin Platform `tm-admin` dan satu tenant lama pada `localhost/tm-legacy`.
2. Dari root repository, terapkan fixture sebelum migrasi 12. Arahkan `psql` ke database uji.

   ```powershell
   psql -X -v ON_ERROR_STOP=1 -f bruno/tenant-management/pre-migration-fixture.sql
   psql -X -v ON_ERROR_STOP=1 -f backend/src/main/resources/db/12-tenant-management.sql
   ```

3. Jalankan backend pada database tersebut dengan `APP_PLATFORM_HOST=localhost`. Dari `bruno/`, jalankan perintah berikut. Hilangkan `--env-var baseUrl=...` jika backend memakai port 8080.

   ```powershell
   bru run smoke --env Local --env-var baseUrl=http://localhost:18081 --bail --noproxy --sandbox developer
   bru run tenant-management/TM-AT-01-list-and-migration --env Local --env-var baseUrl=http://localhost:18081 --bail --noproxy --sandbox developer
   bru run tenant-management/TM-AT-02-action-permissions --env Local --env-var baseUrl=http://localhost:18081 --bail --noproxy --sandbox developer
   bru run tenant-management/TM-AT-03-public-catalog --env Local --env-var baseUrl=http://localhost:18081 --bail --noproxy --sandbox developer
   bru run tenant-management/TM-AT-04-internal-sessions --env Local --env-var baseUrl=http://localhost:18081 --bail --noproxy --sandbox developer
   ```

Setiap skenario membuat fixture HTTP dengan sufiks unik. Untuk memilih sufiks yang mudah ditemukan saat menjalankan TM-AT-03, ekspor `DAGANTARA_TM_RUN_ID` dengan 1–20 karakter huruf kecil atau angka yang belum pernah dipakai pada database itu. Buang database fixture setelah pengujian selesai.

## TM-AT-05 di browser

Pakai dua tenant berproduk terbit dari TM-AT-03. Tenant bernama `TM AT03 domain <sufiks>` memakai `tm-at03-<sufiks>.localhost` dan `TM AT03 slug <sufiks>` memakai `localhost/tm-at03-<sufiks>`. Gunakan port backend uji.

1. Tanpa login di browser, buka `/products` relatif terhadap kedua alamat. Buka tautan produk di setiap toko dan simpan URL detailnya.
2. Dari klien API terpisah dengan akses Admin Platform, panggil `POST /api/v1/tenants/{tenantId}/deactivate` untuk tenant domain. Muat ulang daftar dan buka URL detail secara langsung. Keduanya harus menampilkan “Toko tidak tersedia” tanpa katalog dengan HTTP `404`.
3. Panggil `POST /api/v1/tenants/{tenantId}/reactivate`. Muat ulang daftar dan buka lagi URL detail yang sama. Produk harus kembali tampil dengan HTTP `200`.
4. Ulangi siklus pada tenant dengan jalur toko.

## Hasil verifikasi

Verifikasi lokal 25 November 2026 memakai PostgreSQL 16, backend di port 18081, Bruno CLI 4.3.0, dan browser Orca.

| Skenario | Hasil | Bukti |
| --- | --- | --- |
| TM-AT-01 | Lulus | Tenant dari pengajuan, onboarding langsung, dan fixture sebelum migrasi tercantum di daftar. Dua toko bernama sama diurutkan berdasarkan ID pada halaman terpisah. Tenant yang dinonaktifkan tetap tercantum. |
| TM-AT-02 | Lulus | Izin baca dan dua izin tindakan dapat didelegasikan secara terpisah. Permintaan tanpa akses menghasilkan `401` atau `403`, dan target yang tidak dikenal menghasilkan `404` bagi operator yang berizin. Permintaan penonaktifan paralel dan pengulangan aksi hanya menaikkan versi sesi sekali. |
| TM-AT-03 | Lulus | API katalog dan detail kedua jenis alamat mengembalikan `404` saat nonaktif. Penggunaan ulang alamat menghasilkan `409 LOGIN_URL_IN_USE`. Katalog pembanding tetap tersedia dan katalog asal kembali dengan data yang sama setelah aktivasi. |
| TM-AT-04 | Lulus | Login dan sesi pemilik serta staf ditolak saat tenant nonaktif. Sesi lama tetap ditolak setelah aktivasi. Akun aktif bisa login lagi, akun nonaktif tetap ditolak, dan kewajiban ganti password sementara tetap berlaku. Data katalog tetap tersedia. |
| TM-AT-05 | Lulus | Browser tanpa login merender daftar dan detail untuk kedua alamat. Keduanya menampilkan “Toko tidak tersedia” tanpa katalog saat `404` dan kembali menampilkan produk pada URL yang sama saat `200`. |

Smoke test Bruno lulus. Build `mvnw.cmd -B -ntp package` menjalankan 34 tes Spring Boot pada PostgreSQL tanpa kegagalan atau tes yang dilewati.
