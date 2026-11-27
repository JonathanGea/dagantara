# Fondasi frontend Dagantara (0A)

Workspace Angular ini memiliki tiga aplikasi. Setiap aplikasi punya target dev,
target build, dan stylesheet Tailwind CSS sendiri. Konfigurasi PostCSS dan
lockfile npm dipakai bersama.

## Instalasi

Gunakan Node.js **24.15.0** (lihat `.nvmrc`) dan npm **11.12.1** (lihat
`package.json`). Lockfile mengunci Angular **22.3.0**, TypeScript **6.0.3**,
dan Tailwind CSS **4.3.5**. Jalankan dari root repository.

```powershell
cd frontend
npm ci
```

## Menjalankan dan membangun aplikasi

Jalankan perintah berikut dari `frontend/`. Setiap aplikasi dapat dijalankan
sendiri dan menghasilkan direktori build sendiri.

| Aplikasi | Dev | URL lokal | Build produksi | Output |
| --- | --- | --- | --- | --- |
| Platform Admin | `npm run dev:platform-admin` | `http://localhost:4200` | `npm run build:platform-admin` | `dist/platform-admin/browser/` |
| Tenant Admin | `npm run dev:tenant-admin` | `http://localhost:4201` | `npm run build:tenant-admin` | `dist/tenant-admin/browser/` |
| Storefront | `npm run dev:storefront` | `http://localhost:4202` | `npm run build:storefront` | `dist/storefront/browser/` |

Ketiganya menampilkan judul aplikasi di kartu putih dengan latar abu-abu muda
untuk memeriksa Tailwind. Masing-masing stylesheet memindai direktori
`src/` aplikasi yang bersangkutan.

## Koneksi API lokal

Ikuti [panduan menjalankan backend](../backend/README.md#development). Ketiga
dev server memakai [konfigurasi proxy yang sama](proxy.conf.json) dengan tujuan
`http://localhost:8080`. Ubah `target` dan mulai ulang dev server jika backend
memakai port lain.

- Gunakan URL relatif terhadap origin, misalnya `/api/v1/users` untuk akses
  platform atau `/{shopSlug}/api/v1/tenant/users` untuk akses tenant dengan
  prefiks toko. Proxy meneruskan `/api/**` dan `/*/api/**`, termasuk path, query,
  dan header `Host` asli. Backend tetap memvalidasi konteks URL toko dan izin akun.
- Dengan backend aktif, `GET /actuator/health` pada tiap URL dev mengembalikan
  respons Spring Boot. Respons backend sehat memiliki HTTP 200 dan status `UP`.
  Untuk smoke test backend langsung, jalankan `bru run smoke --env Local`
  dari `bruno/`.

Proxy ini berlaku saat `ng serve`. Pemetaan host dan prefiks URL untuk akses
Platform Admin, Tenant Admin, dan Storefront dilanjutkan di
[0B](../docs/planning/progress.md#fondasi-angular-dan-tailwind). Pola UI bersama
dan layout ditinjau di 0C dan 1A–1C.

## Verifikasi 0A

`npm ci --no-audit --no-fund` berhasil dari salinan workspace di direktori
sementara yang bersih. Ketiga perintah dev dan ketiga build produksi di tabel
berhasil. Di browser Orca, tiap aplikasi menampilkan judul 30 px dan kartu putih
dengan padding 32 px di atas latar abu-abu muda.

Server HTTP sementara di port 8080 mengonfirmasi bahwa ketiga proxy
meneruskan path API biasa, path dengan prefiks toko, dan health check tanpa
mengubah path, query, atau `Host`. Uji terhadap Spring Boot dan smoke test
Bruno tertahan karena backend lokal belum aktif. PostgreSQL bawaan di port
5433 tidak tersedia dan instance di port 5432 menolak kredensial aplikasi
lokal yang diperiksa.
