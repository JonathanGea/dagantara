# Store Settings 02 — Logo toko

Spec induk: [Store Settings](../../specs/store-settings.md)

Status: done

## What to Build

Implementasikan SS-02 dan bagian logo dari SS-03, SS-04, dan SS-05. Lengkapi form dan API admin dengan `KEEP`, `REPLACE`, dan `REMOVE`. Validasi satu gambar PNG, JPEG, atau WebP hingga 2 MiB berdasarkan isi. Simpan file secara lokal dan terbitkan referensi aktif bersama nama dan kontak dalam satu transaksi. Sajikan logo aktif pada URL toko dan perbarui header daftar serta detail.

## Acceptance Criteria

- [x] SS-AT-03 — Unggah logo valid dan periksa tampilan pada daftar dan detail. `KEEP` mempertahankan URL logo. Logo tetap tersedia setelah restart dengan direktori yang sama. Penggantian dan penghapusan menonaktifkan URL lama. Jika file aktif hilang, endpoint memberi `404` dan header tetap menampilkan nama toko.
- [x] SS-AT-04 — File yang tidak valid menghasilkan `400`, dan gambar di atas 2 MiB menghasilkan `413`. Kontak tidak valid dan kegagalan penyimpanan file mempertahankan semua nilai lama serta logo aktif. Pesan error tidak mengungkap lokasi file.
- [x] Bagian logo SS-AT-06 — Logo A dan B hanya tersedia melalui URL tenant masing-masing. ID logo A pada URL B atau URL yang tidak terpetakan mendapat `404`.
- [x] Kontrak aksi logo menolak file pada `KEEP` atau `REMOVE` dan mewajibkan satu file untuk `REPLACE`. Setelah perubahan bersamaan, hanya referensi aktif dapat diakses dan pembersihan tidak menghapus file aktif.
- [x] Jalankan Bruno dengan `bru run --env Local`, tes integrasi kegagalan penyimpanan, dan verifikasi browser terhadap kedua halaman serta setelah restart. Catat perintah dan hasil per skenario.

## Bukti verifikasi

Basis child `feat/store-settings-02` memuat tiket 01 berstatus `done` pada commit `653e691`. Verifikasi memakai PostgreSQL 16 terisolasi pada port `15440`, backend JDK 25 pada port `18090`, dan `APP_STORE_SETTINGS_LOGO_DIRECTORY` yang menunjuk ke `%LOCALAPPDATA%\Dagantara-SS02-Test\browser-logos`. Terapkan migrasi 01–09, fixture dua tenant, lalu migrasi 10 dan 11 dengan `psql -X -v ON_ERROR_STOP=1 -f <file>`. Tenant A memakai `127.0.0.1:18090` dan tenant B memakai `localhost:18090/ss01-b`. [Fixture Bruno](../../../bruno/store-settings/fixture.sql)

Dari `backend/`, `.\mvnw.cmd -B -ntp verify` menghasilkan `BUILD SUCCESS`. Seluruh 34 tes lulus tanpa skip, termasuk 9 tes Store Settings yang berjalan pada schema dan port acak. Setel akhiran `URL`, `USERNAME`, dan `PASSWORD` untuk masing-masing awalan `STORE_SETTINGS01_TEST_DB_`, `PRODUCT01_TEST_DB_`, `PRODUCT03_TEST_DB_`, dan `INVENTORY01_TEST_DB_` ke PostgreSQL uji.

Dari `bruno/`, jalankan perintah berikut dengan `SS01_FIXTURE_PASSWORD` yang sama dengan fixture. Smoke lulus 2/2 dan koleksi Store Settings lulus 9/9. Keempat skenario logo juga lulus saat dijalankan sendiri.

```powershell
npx --yes --package @usebruno/cli@4.3.0 bru run smoke --env Local --env-var baseUrl=http://localhost:18090 --bail --noproxy
npx --yes --package @usebruno/cli@4.3.0 bru run store-settings -r --env Local --env-var baseUrl=http://localhost:18090 --bail --noproxy --sandbox developer

foreach ($scenario in @('SS-AT-03-siklus-logo', 'SS-AT-04-validasi-logo', 'SS-AT-06-isolasi-logo', 'SS-02-kontrak-aksi-logo')) {
    npx --yes --package @usebruno/cli@4.3.0 bru run "store-settings/$scenario" --env Local --env-var baseUrl=http://localhost:18090 --bail --noproxy --sandbox developer
}
```

Backend pertama dijalankan dengan `run-backend.ps1 -Port 18090 -Foreground`. Hentikan proses lalu jalankan kembali dengan `java -jar target/backend-0.0.1-SNAPSHOT.jar --server.port=18090` dari `backend/` dengan database dan direktori logo yang sama. Pemeriksaan `/actuator/health` mengembalikan `UP` pada kedua proses. Browser Orca memakai `orca tab create --url <url>`, `orca fill`, `orca upload --page <id> --element <ref> --files <path>`, serta `orca click` dan `orca check` untuk menjalankan form. Pemeriksaan halaman memakai `orca goto`, `orca snapshot`, dan `orca eval` untuk memeriksa URL gambar dan `naturalWidth` pada daftar dan detail.

| Kriteria | Hasil |
| --- | --- |
| SS-AT-03 | Bruno membuktikan perubahan dan pembacaan ulang untuk `REPLACE`, `KEEP`, dan `REMOVE`. Spring menguji PNG, JPEG, dan WebP dengan nama dan tipe unggahan yang sengaja berbeda, serta memeriksa tipe respons, byte gambar, dan cache `no-store`. Browser menampilkan logo di form, daftar, dan detail sesudah unggah. `KEEP` dan restart mempertahankan URL, gambar tetap termuat pada kedua halaman kedua tenant, dan checksum PNG A cocok dengan fixture asli. Penggantian menampilkan JPEG baru dan penghapusan menyembunyikan gambar. Kedua URL yang dinonaktifkan memberi `404`. Setelah file aktif dipindahkan sementara, endpoint memberi `404`, sementara nama toko tetap terlihat pada daftar dan detail. |
| SS-AT-04 | Bruno menerima `400` untuk gambar rusak dan kontak tidak valid serta `413` untuk PNG di atas 2 MiB. Baca ulang admin, gambar, daftar, dan detail menunjukkan pengaturan sebelumnya. Browser menampilkan pesan gagal saat file pengganti tidak bisa dibaca. Tes integrasi memaksa kegagalan penulisan file dan pelanggaran constraint database. Pengaturan lama bertahan, logo lama masih dapat disajikan setelah direktori uji dikembalikan, dan file kandidat dibersihkan saat transaksi gagal. Pesan penyimpanan tidak memuat lokasi file. |
| SS-AT-06 (logo) | Bruno mendapat `200` dan gambar yang sesuai di URL masing-masing tenant. ID logo A memberi `404` melalui toko B atau prefiks yang tidak terdaftar. ID B memberi `404` melalui hostname A. Di browser, perubahan logo A tidak mengubah gambar, nama, kontak, atau produk B pada daftar dan detail. |
| Kontrak aksi dan konkurensi | Bruno menerima `400` untuk file bersama `KEEP` atau `REMOVE`, serta `REPLACE` tanpa file atau dengan dua file. Setelah dua penggantian bersamaan, hanya URL aktif memberi `200`. Spring menguji dua penggantian dan `KEEP` secara bersamaan saat record belum ada. Hanya satu record dan satu file aktif tersisa. Penghapusan menghasilkan `404` dan membersihkan file. |

## Verifikasi gabungan

Kedua tiket diverifikasi dari branch `jonathan/feat-store-settings` setelah sinkronisasi dengan `origin/dev`. Pengujian memakai JDK 25, PostgreSQL 16 terisolasi pada port `15441`, backend pada port `18091`, dan direktori logo uji terpisah. Migrasi 01–09, fixture dua tenant, serta migrasi 10–11 diterapkan pada database bersih.

Dari `backend/`, `.\mvnw.cmd -B -ntp verify` menghasilkan `BUILD SUCCESS` dengan 34 tes lulus tanpa skip. Dari `bruno/`, kedua perintah berikut lulus untuk seluruh skenario HTTP SS-AT-01 hingga SS-AT-06 dan kontrak aksi logo.

```powershell
npx --yes --package @usebruno/cli@4.3.0 bru run smoke --env Local --env-var baseUrl=http://localhost:18091 --bail --noproxy
npx --yes --package @usebruno/cli@4.3.0 bru run store-settings -r --env Local --env-var baseUrl=http://localhost:18091 --bail --noproxy --sandbox developer
```

Smoke lulus 2/2 dan koleksi Store Settings lulus 9/9. Di browser Orca, admin tenant A masuk dan menyimpan nama, WhatsApp, email, serta PNG lewat form. Daftar dan detail di hostname langsung dan URL berprefiks menampilkan nama, tautan kontak, produk, dan logo masing-masing tenant. Setelah JAR dimulai ulang dengan database dan direktori yang sama, keempat halaman menampilkan kembali logo yang termuat dari URL yang sama. Pemeriksaan health menghasilkan `UP` sebelum dan sesudah restart. [Bukti nama dan kontak](01-nama-dan-kontak-publik.md#bukti-verifikasi)

## Blocked By

- [Store Settings 01](01-nama-dan-kontak-publik.md) menyediakan form, permission, penyimpanan pengaturan per tenant, dan header storefront.
