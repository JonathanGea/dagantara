---
name: implement-from-spec
description: Implementasikan spec perubahan atau tiket Dagantara berdasarkan judul, slug, atau path. Dukung juga spesifikasi modul, requirement, dan skenario uji.
---

# Implementasi dari Spesifikasi Dagantara

## Tetapkan cakupan

1. Baca `AGENTS.md` dan `docs/README.md`. Temukan spesifikasi dari judul, slug `docs/<slug>.md`, path, atau ID yang disebut pengguna. Jika ada beberapa kecocokan setelah mencari di repo, minta pengguna memilih. Baca aturan domain yang menjadi rujukan dan `backend/AGENTS.md` untuk perubahan backend. Ikuti cakupan permintaan analisis atau review jika disebutkan.
2. Untuk spec perubahan, petakan user stories, keputusan implementasi, keputusan pengujian, dan batas cakupan. `$implement-from-spec <slug>` menjalankan seluruh spec yang bisa dikerjakan langsung. Jika bagian **Implementation Tickets** menunggu persetujuan, laporkan statusnya. Jika bagian itu menautkan tiket yang sudah disetujui dan pengguna belum memilih tiket, tampilkan tiket yang bisa dimulai beserta perintahnya.
3. Untuk `$implement-from-spec <slug> <NN>`, baca spec induk dan tiket bernomor tersebut dari daftar tiket yang disetujui. Gunakan hasil dan Acceptance Criteria tiket sebagai cakupan. Periksa bahwa tiket masih `ready-for-agent`. Jika sudah `done`, laporkan statusnya. Setiap prasyarat harus `done` dan hasilnya tersedia di basis branch kerja sebelum coding. Laporkan prasyarat yang masih menghalangi jika ada.
4. Untuk format modul, path atau nama modul saja memilih seluruh cakupan yang disepakati. Aksi, ID requirement, atau ID skenario mempersempit cakupan. Petakan Functional Specification, Business Rules, Data Model, dan Acceptance Scenarios yang relevan. Admin UI Structure menunjukkan lokasi aksi yang terhubung ke requirement. Jika permintaan mengubah keputusan yang terdokumentasi, sebutkan konflik yang perlu dituntaskan.

## Siapkan branch kerja di Dagantara

1. Untuk setiap tugas implementasi baru, cek `git status --short --branch` dan jalankan `git fetch origin` sebelum mengubah file.
2. Sinkronkan `dev` lokal dengan `origin/dev` secara fast-forward. Jika keduanya sudah sama atau dapat disamakan dengan aman, jalankan `git switch dev`, `git merge --ff-only origin/dev`, lalu `git switch -c <branch-kerja>`. Gunakan nama branch yang mencerminkan ID atau fitur yang dikerjakan.
3. Jika perubahan lokal harus dipertahankan, `dev` sedang digunakan di worktree lain, atau `dev` berisi commit lokal yang belum ada di remote, buat branch kerja baru di worktree terpisah langsung dari `origin/dev` yang sudah diperbarui. Pertahankan perubahan dan riwayat lokal yang ada.
4. Jika spec atau tiket yang menjadi dasar pekerjaan baru tersedia di workspace asal, sertakan dokumen dan rujukan yang diperlukan pada branch kerja agar sesi baru bisa melanjutkan pekerjaan. Bawa hanya file yang terkait dan pertahankan versi lain yang sudah ada di branch tujuan. Periksa kelengkapan dokumen dan hasil prasyarat sebelum implementasi.

## Implementasikan cakupan yang disepakati

1. Periksa kode, skema, kontrak API, dan tes yang terkait dengan cakupan. Temukan jalur yang sudah ada dan perubahan yang dibutuhkan. Pilih skill dari tabel routing di `AGENTS.md` sesuai pekerjaan. Baca skill saat pertama kali diperlukan, lalu gunakan kembali instruksi dan dokumen yang sudah tersedia dalam konteks tugas.
2. Bedakan keputusan teknis yang dapat mengikuti pola repo dari keputusan domain yang belum disepakati. Untuk bagian yang bergantung pada keputusan domain, sebutkan ID, informasi yang kurang, dan bagian yang tertahan. Lanjutkan bagian lain dalam cakupan yang dapat dikerjakan secara mandiri. Hindari menambah kontrak, permission, atau skema yang mengunci keputusan terbuka.
3. Ikuti batas akses dari aturan bisnis. Saat menyentuh data tenant, telusuri konteks tenant pelaku, kepemilikan target, dan kepemilikan entitas yang ditautkan. Saat mengubah beberapa relasi, pertahankan invarian model pada akhir transaksi dan pastikan operasi yang ditolak tidak meninggalkan perubahan parsial.

## Verifikasi terhadap spesifikasi

- Verifikasi perilaku melalui antarmuka uji yang disepakati di spec. Gunakan Testing Decisions dan Acceptance Criteria untuk spec atau tiket, serta Acceptance Scenarios untuk format modul. Cek hasil sukses dan penolakan yang tercakup, termasuk keadaan data setelah penolakan. Jalankan tes dan build yang tersedia untuk perubahan tersebut.
- Untuk perubahan perilaku API HTTP, ikuti `bruno-api-testing` untuk menulis dan menjalankan skenario penerimaan lokal dalam cakupan. Skill tersebut menggunakan `spring-boot-run` jika backend perlu disiapkan. Gunakan `spring-boot-testing` untuk tes Spring Boot.
- Laporkan hasil per kriteria atau ID skenario beserta perintah dan keputusan yang masih diperlukan. Nyatakan cakupan selesai setelah perilaku yang disepakati tercakup dan semua pemeriksaan wajib lulus. Untuk tiket, tandai kriteria yang terverifikasi dan ubah status menjadi `done` dalam perubahan yang sama. Sebutkan tiket lanjutan yang prasyaratnya lengkap setelah hasil ini tersedia di branch yang akan dipakai.

## Buka PR ke `dev` di Dagantara

1. Setelah verifikasi, tinjau diff, commit file yang termasuk dalam tugas, lalu push branch kerja ke `origin` dengan `git push -u origin <branch-kerja>`.
2. Buka PR GitHub dengan base `dev` dan head branch kerja, misalnya lewat `gh pr create --base dev --head <branch-kerja> --title <judul> --body-file <file-body>`. Isi PR dengan ringkasan perubahan, ID requirement dan skenario yang dicakup, perintah dan hasil verifikasi, serta dependensi atau batas verifikasi yang masih tersisa. Jika PR untuk branch itu sudah terbuka, gunakan PR tersebut.
3. Pastikan PR terbuka dengan target `dev`. Laporkan tautan PR, branch, file yang berubah, dan hasil verifikasi. Jika push atau pembukaan PR terhambat, laporkan langkah yang tertahan dan alasannya.

## Contoh penerapan di Dagantara

- Pembuatan user mengikuti `UM-01` dan `UM-02`, dengan `UM-03` dan `UM-06` sebagai aturan terkait. Gunakan `UM-AT-01` dan `UM-AT-02` untuk memeriksa tenant, satu role aktif, dan penolakan jika pelaku tidak punya `users.create`. `tenant_id = NULL` pada akun platform tetap tunduk pada aturan izin yang terdokumentasi.
- Asosiasi media dengan nilai opsi mengikuti `PM-03` dan `PM-04`. Gunakan `PM-AT-03` dan `PM-AT-04` untuk memeriksa relasi many-to-many dan kepemilikan media oleh produk. Jika tugas juga mencakup `PM-02`, cek status keputusan penyimpanan Primary Category di spesifikasi sebelum mengubah skema atau menutup `PM-AT-02`.
