# Panduan Penggunaan Skill

Buka chat Codex di root proyek. Tekan **Copy** pada blok prompt yang sesuai, lalu tempel ke chat. Awalan `$nama-skill` memilih skill. Sertakan tujuan, dokumen atau ID requirement jika ada, dan hasil yang ingin kamu lihat.

## Pilih alur kerja

- **Mulai dari ide fitur.** Gunakan `$write-module-spec` untuk menyepakati perilaku dan pengujian. Gunakan `$design-ui` jika perlu meninjau layar atau interaksi. Lanjutkan dengan `$implement-from-spec` saat cakupan siap dikerjakan.
- **Mulai dari layout dasar atau aplikasi yang sudah ada.** Gunakan `$design-ui` dengan brief atau rujukan layar. Bawa keputusan yang mengubah fitur, izin, atau aturan bisnis ke pembahasan spesifikasi sebelum implementasi.
- **Kerjakan atau verifikasi backend.** Gunakan skill API, database, security, atau testing sesuai cakupan. Gunakan skill penyiapan PostgreSQL atau menjalankan backend untuk kebutuhan lingkungan lokal.

## $write-module-spec

Gunakan saat ide atau perubahan perilaku masih perlu dibahas. Skill ini merangkum keputusan untuk dikonfirmasi, lalu menulis spesifikasi dan mengusulkan pembagian tiket jika perlu. [Instruksi skill](../viewer/skills/write-module-spec.md)

```text
$write-module-spec Bahas fitur undangan pengguna lewat email untuk Dagantara.
Mulai dari aturan User Management yang berlaku. Bahas siapa yang boleh mengundang,
kapan akun dibuat, dan bagaimana undangan kedaluwarsa. Beri saya rekomendasi per
keputusan dan rangkum skenario uji sebelum meminta konfirmasi untuk menulis spec.
```

## $design-ui

Gunakan untuk merancang layout, halaman, atau alur yang akan ditinjau. Mulai dari brief, aplikasi yang berjalan, atau spec. Untuk memakai skill ini di proyek lain, salin folder `.agents/skills/design-ui` ke lokasi skill yang dipakai proyek atau akun Codex kamu. [Instruksi skill](../viewer/skills/design-ui.md)

**Contoh layout dasar tanpa spec**

```text
$design-ui Buat prototipe HTML untuk portal reservasi yang dipakai pelanggan di HP.
Pelanggan perlu melihat jadwal dan memilih satu slot. Buat data contoh dan alur
yang bisa diklik sampai slot terpilih. Catat asumsi dan cara mencoba prototipenya.
```

**Contoh alur dari requirement Dagantara**

```text
$design-ui Rancang alur Tambah Pengguna dari UM-01 dan UM-02 di User Management.
Buat prototipe HTML untuk pengisian formulir, pemilihan role, dan umpan balik
saat penyimpanan ditolak. Catat skenario UI yang perlu diuji saat implementasi.
```

**Contoh revisi aplikasi yang sudah berjalan**

```text
$design-ui Tinjau layar pencarian yang ada di proyek ini dari sudut pandang
pengguna ponsel. Gunakan pola navigasi dan komponen yang sudah dipakai. Buat
wireframe untuk pencarian, hasil, dan kondisi tanpa hasil untuk saya tinjau.
```

## $implement-from-spec

Gunakan untuk membangun dan memverifikasi cakupan yang disepakati. Pilih spec lewat slug, tiket lewat slug dan nomor, atau cakupan modul lewat path dan ID requirement. Untuk tiket, pastikan statusnya `ready-for-agent` dan prasyaratnya sudah tersedia di branch dasar. Pengujian API HTTP dalam cakupan dijalankan dengan `$bruno-api-testing` sebagai bagian dari verifikasi. [Instruksi skill](../viewer/skills/implement-from-spec.md)

```text
$implement-from-spec @docs/specs/user-management.md UM-01 UM-02
Fokus pada pembuatan pengguna dan penetapan tenant serta role. Verifikasi
UM-AT-01 dan UM-AT-02. Laporkan keputusan yang masih menghalangi implementasi
atau pengujian, lalu lanjutkan bagian yang cakupannya sudah jelas.
```

Di sesi Orca, panggil spec yang menautkan tiket yang sudah disetujui untuk mengoordinasikan seluruh tiket. Koordinator membaca kondisi branch dan worktree untuk melanjutkan pekerjaan yang sudah ada. Ia menjalankan satu worker Codex CLI per child. GPT-6.2 Sol adalah default, dengan effort `medium` untuk tiket rutin atau `high` untuk perubahan berisiko tinggi. GPT-6 Luna dengan `high` bisa dipakai untuk tiket kecil dengan kriteria penerimaan yang jelas dan mudah diverifikasi. Tiket independen berjalan paralel. Tambahkan nomor tiket untuk mengerjakan satu tiket saja.

```text
$implement-from-spec @docs/specs/user-management.md
```

## $spring-boot-api

Gunakan untuk mengerjakan atau meninjau controller, DTO, validasi input, serta kontrak respons dan error. Contoh ini meminta peninjauan kontrak yang tersedia. [Instruksi skill](../viewer/skills/spring-boot-api.md)

```text
$spring-boot-api Tinjau kontrak REST untuk pembuatan pengguna pada UM-01 dan UM-02.
Periksa controller, DTO, validasi request, dan respons error yang sudah ada.
Laporkan yang sesuai dengan spec dan tunjukkan keputusan kontrak yang masih terbuka.
```

## $spring-boot-database

Gunakan untuk merancang atau mengubah migration, entity, relasi, query, dan transaksi. Hubungkan perubahan data dengan aturan dan skenario yang sudah diputuskan. [Instruksi skill](../viewer/skills/spring-boot-database.md)

```text
$spring-boot-database Rancang perubahan PostgreSQL dan JPA untuk PM-03 dan PM-04.
Periksa bagaimana media produk bisa ditautkan ke beberapa nilai opsi pada produk
yang sama. Tunjukkan constraint dan batas transaksi yang diperlukan untuk
PM-AT-03 dan PM-AT-04.
```

## $spring-boot-security

Gunakan untuk autentikasi, pemetaan role dan permission, serta isolasi tenant. Contoh ini meninjau keamanan alur tambah pengguna. [Instruksi skill](../viewer/skills/spring-boot-security.md)

```text
$spring-boot-security Tinjau izin pada alur tambah pengguna berdasarkan UM-01,
UM-02, dan UM-06. Telusuri cara backend menentukan permission users.create,
konteks tenant, dan scope role target. Laporkan pemeriksaan yang sudah tersedia
dan bagian yang masih tergantung pada keputusan domain.
```

## $create-postgres-database

Gunakan untuk menyiapkan PostgreSQL pengembangan Dagantara di Windows. Contoh ini memakai instance lokal bawaan dan meminta verifikasi koneksi dengan akun aplikasi. [Instruksi skill](../viewer/skills/create-postgres-database.md)

```text
$create-postgres-database Siapkan PostgreSQL lokal Dagantara di Windows dengan
konfigurasi bawaan. Gunakan ulang instance lokal jika sudah tersedia. Verifikasi
koneksi ke database dagantara sebagai dagantara_app dan laporkan host serta port
tanpa menampilkan kata sandi.
```

## $spring-boot-run

Gunakan untuk menyalakan atau memeriksa backend lokal. Prompt ini meminta alamat untuk pengujian berikutnya dan membiarkan proses tetap berjalan. [Instruksi skill](../viewer/skills/spring-boot-run.md)

```text
$spring-boot-run Jalankan backend Dagantara secara lokal untuk pengujian Bruno.
Jika sudah berjalan, periksa statusnya. Laporkan Base URL dan hasil pemeriksaan
health jika tersedia, lalu biarkan backend tetap berjalan.
```

## $spring-boot-testing

Gunakan untuk menulis atau menjalankan unit, MVC, integration, repository, dan security test Spring Boot. [Instruksi skill](../viewer/skills/spring-boot-testing.md)

```text
$spring-boot-testing Jalankan tes Spring Boot untuk perubahan yang sedang
dikerjakan. Laporkan perintah, hasil, dan kriteria penerimaan yang dibuktikan.
```

## $bruno-api-testing

Gunakan untuk menulis atau menjalankan smoke test dan skenario penerimaan API HTTP. Periksa status, isi respons yang dijanjikan, dan keadaan data sesudah aksi bila relevan. [Instruksi skill](../viewer/skills/bruno-api-testing.md)

```text
$bruno-api-testing Jalankan smoke test Dagantara dengan environment Local.
Siapkan backend lokal bila diperlukan. Jalankan dari folder bruno dan
laporkan perintah serta hasilnya.
```
