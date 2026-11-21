---
name: implement-from-spec
description: Gunakan untuk mengimplementasikan seluruh modul, fitur, atau acceptance scenario dari spesifikasi modul Markdown yang memuat Functional Specification, Business Rules, Data Model, dan Acceptance Scenarios, seperti dokumentasi modul Dagantara.
---

# Implementasi dari Spesifikasi Modul

## Tetapkan cakupan

1. Jika pengguna memanggil skill dengan hanya satu file modul, seperti `$implement-from-spec @docs/user-management.md`, implementasikan seluruh modul tersebut. Jika pengguna menyebutkan aksi, ID, atau batas pekerjaan, ikuti cakupan yang diberikan. Untuk permintaan analisis atau review, ikuti tujuan tersebut.
2. Baca `AGENTS.md`, `docs/README.md`, dan file modul yang dirujuk. Untuk perubahan backend, baca juga `backend/AGENTS.md`. Gunakan `docs/user-management.md` untuk ID `UM-*` dan `docs/product-management.md` untuk ID `PM-*`. Baca spesifikasi modul lain jika ada dependensi lintas modul.
3. Petakan Functional Specification, Business Rules, Data Model, dan Acceptance Scenarios yang relevan. Untuk satu modul penuh, petakan seluruh cakupan yang sudah disepakati sebelum coding. Urutkan pengerjaan per alur berdasarkan dependensi dan lacak tiap ID hingga verifikasi akhir. Untuk aksi tanpa ID, temukan ID dari tindakan yang diminta.
4. Gunakan Admin UI Structure untuk menemukan lokasi aksi yang sudah dispesifikasikan. Perlakukan nama menu, contoh, dan diagram yang ditandai sebagai referensi sebagai konteks. Ambil perilaku, relasi, dan constraint yang mengikat dari keputusan yang tercatat di Markdown modul. Jika permintaan eksplisit mengubah keputusan terdokumentasi, sebutkan perubahan tersebut dan tandai konflik yang masih perlu diselesaikan.

## Siapkan branch kerja di Dagantara

1. Untuk setiap tugas implementasi baru, cek `git status --short --branch` dan jalankan `git fetch origin` sebelum mengubah file.
2. Sinkronkan `dev` lokal dengan `origin/dev` secara fast-forward. Jika keduanya sudah sama atau dapat disamakan dengan aman, jalankan `git switch dev`, `git merge --ff-only origin/dev`, lalu `git switch -c <branch-kerja>`. Gunakan nama branch yang mencerminkan ID atau fitur yang dikerjakan.
3. Jika perubahan lokal harus dipertahankan, `dev` sedang digunakan di worktree lain, atau `dev` berisi commit lokal yang belum ada di remote, buat branch kerja baru di worktree terpisah langsung dari `origin/dev` yang sudah diperbarui. Pertahankan perubahan dan riwayat lokal yang ada.

## Implementasikan cakupan yang disepakati

1. Periksa kode, skema, kontrak API, dan tes yang terkait dengan ID dalam cakupan. Temukan jalur yang sudah ada dan perubahan yang dibutuhkan. Pilih skill dari tabel routing di `AGENTS.md` sesuai pekerjaan. Baca skill saat pertama kali diperlukan, lalu gunakan kembali instruksi dan dokumen yang sudah tersedia dalam konteks tugas.
   Untuk backend, tempatkan kode di subpackage sesuai tanggung jawab dalam `modules/<nama_module>` mengikuti struktur di `backend/AGENTS.md`.
2. Bedakan keputusan teknis yang dapat mengikuti pola repo dari keputusan domain yang belum disepakati. Untuk bagian yang bergantung pada keputusan domain, sebutkan ID, informasi yang kurang, dan bagian yang tertahan. Lanjutkan bagian lain dalam cakupan yang dapat dikerjakan secara mandiri. Hindari menambah kontrak, permission, atau skema yang mengunci keputusan terbuka.
3. Ikuti batas akses dari aturan bisnis. Saat menyentuh data tenant, telusuri konteks tenant pelaku, kepemilikan target, dan kepemilikan entitas yang ditautkan. Saat mengubah beberapa relasi, pertahankan invarian model pada akhir transaksi dan pastikan operasi yang ditolak tidak meninggalkan perubahan parsial.

## Verifikasi terhadap spesifikasi

- Gunakan kondisi, tindakan, dan hasil masing-masing Acceptance Scenario untuk memilih tes yang relevan. Cek hasil sukses dan penolakan jika tercakup, termasuk keadaan data setelah penolakan. Jalankan tes dan build yang tersedia untuk perubahan tersebut.
- Untuk pengujian API HTTP, ikuti `spring-boot-testing`. Gunakan `spring-boot-run` jika perlu menjalankan backend lokal, lalu jalankan smoke test Bruno. Setelah smoke test lulus, jalankan semua skenario HTTP dalam cakupan tugas.
- Laporkan hasil verifikasi per ID Acceptance Scenario beserta perintah dan keputusan yang masih diperlukan. Nyatakan modul selesai setelah seluruh requirement yang disepakati tercakup dan semua skenario wajib lulus.

## Buka PR ke `dev` di Dagantara

1. Setelah verifikasi, tinjau diff, commit file yang termasuk dalam tugas, lalu push branch kerja ke `origin` dengan `git push -u origin <branch-kerja>`.
2. Buka PR GitHub dengan base `dev` dan head branch kerja, misalnya lewat `gh pr create --base dev --head <branch-kerja> --title <judul> --body-file <file-body>`. Isi PR dengan ringkasan perubahan, ID requirement dan skenario yang dicakup, perintah dan hasil verifikasi, serta dependensi atau batas verifikasi yang masih tersisa. Jika PR untuk branch itu sudah terbuka, gunakan PR tersebut.
3. Pastikan PR terbuka dengan target `dev`. Laporkan tautan PR, branch, file yang berubah, dan hasil verifikasi. Jika push atau pembukaan PR terhambat, laporkan langkah yang tertahan dan alasannya.

## Contoh penerapan di Dagantara

- Pembuatan user mengikuti `UM-01` dan `UM-02`, dengan `UM-03` dan `UM-06` sebagai aturan terkait. Gunakan `UM-AT-01` dan `UM-AT-02` untuk memeriksa tenant, satu role aktif, dan penolakan jika pelaku tidak punya `users.create`. `tenant_id = NULL` pada akun platform tetap tunduk pada aturan izin yang terdokumentasi.
- Asosiasi media dengan nilai opsi mengikuti `PM-03` dan `PM-04`. Gunakan `PM-AT-03` dan `PM-AT-04` untuk memeriksa relasi many-to-many dan kepemilikan media oleh produk. Jika tugas juga mencakup `PM-02`, cek status keputusan penyimpanan Primary Category di spesifikasi sebelum mengubah skema atau menutup `PM-AT-02`.
