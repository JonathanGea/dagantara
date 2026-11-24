---
name: write-module-spec
description: Bahas ide perubahan Dagantara dengan pertanyaan dan rekomendasi, lalu tulis spec siap implementasi dan usulkan tiket bila cakupannya besar.
---

# Dari Ide ke Spesifikasi

Terima ide, masalah, atau usulan revisi. Baca `AGENTS.md`, `docs/README.md`, spesifikasi terkait, dan kode atau tes yang relevan. Gunakan istilah domain yang sudah disepakati. Jika ide mengubah aturan yang berlaku, tunjukkan keputusan yang terdampak dan minta pengguna menegaskan perubahan itu.

## Bahas dan konfirmasi desain

1. Temukan fakta dari repo. Petakan keputusan yang masih terbuka dan hubungan prasyaratnya. Telusuri alur nyata, termasuk kasus batas yang bisa mengubah hasil, pihak yang berwenang, kepemilikan tenant, serta dampak terhadap data atau kontrak yang sudah dipakai jika relevan.
2. Per putaran, ajukan keputusan yang prasyaratnya sudah jelas. Beri nomor pada setiap pertanyaan dan sertakan rekomendasi, alasan, serta konsekuensi penting. Tunggu jawaban sebelum membahas keputusan turunannya. Catat keputusan yang sudah dijawab atau secara eksplisit didelegasikan.
3. Gunakan keputusan pengujian yang sudah tercatat dan standar repo. Untuk antarmuka API HTTP, catat Bruno sebagai metode pengujian yang berlaku sesuai `AGENTS.md`. Fokuskan pertanyaan pada hasil yang harus bisa diamati. Bahas pilihan antarmuka uji hanya jika repo belum menentukan pilihannya dan pilihan itu mengubah kriteria penerimaan atau bukti yang dapat diperoleh.
4. Saat keputusan yang memengaruhi hasil dan verifikasi sudah terjawab, rangkum tujuan, perilaku, batas cakupan, dan hasil yang akan diverifikasi. Cantumkan metode pengujian yang berlaku sebagai bagian dari ringkasan. Tunggu konfirmasi pengguna atas desain sebelum menulis spec final. Jika ada keputusan penghambat yang ditunda, identifikasi cakupan yang tertahan dan minta konfirmasi atas cakupan yang bisa diselesaikan.

## Tulis satu spec per perubahan

Simpan sebagai `docs/specs/<slug>.md` dan tautkan dari `docs/README.md`. Pilih judul yang menyatakan hasil dan slug yang unik. Rujuk aturan domain yang sudah ada. Jika pengguna menyetujui perubahan aturan, perbarui juga bagian dokumen sumber yang terdampak dalam cakupan yang sama. Simpan istilah domain baru yang perlu dipakai ulang di `GLOSSARY.md`. Gunakan ADR di `docs/adr/` ketika sebuah keputusan sulit dibalik, membutuhkan konteks, dan lahir dari pertukaran yang nyata.

- **Problem Statement** — Masalah dari sudut pandang pengguna.
- **Solution** — Hasil yang akan dialami pengguna.
- **User Stories** — Daftar bernomor yang menyebut pelaku, kemampuan, dan manfaat. Cakup alur utama dan kasus batas yang disepakati.
- **Implementation Decisions** — Keputusan tentang modul, antarmuka, skema, API, interaksi, atau aturan domain yang memengaruhi hasil. Rujuk sumbernya jika sudah tercatat. Gunakan nama domain dan antarmuka yang stabil.
- **Testing Decisions** — Metode pengujian yang berlaku dan perilaku yang harus teramati. Nyatakan kondisi, aksi, dan hasil untuk kasus sukses dan penolakan yang relevan. Untuk API HTTP, catat penggunaan Bruno dan hasil yang perlu dibuktikan lewat respons serta keadaan data sesudah aksi bila relevan.
- **Out of Scope** — Batas yang disepakati.
- **Further Notes** — Konteks atau referensi tambahan jika diperlukan.

Periksa kesiapan dari sudut pandang sesi baru. Spec dan rujukannya harus cukup untuk menentukan hasil yang dibangun, keputusan yang wajib diikuti, dan cara membuktikan hasil tanpa menggali riwayat percakapan.

Setelah spec versi yang dilacak disetujui dan ditulis, centang **Spec selesai** di `docs/planning/progress.md` dan tautkan spec. Jika modul itu bertanda **Berikutnya dibahas**, pindahkan penanda ke satu modul sesuai urutan roadmap, keputusan penundaan, dan dependensi yang tercatat. Sebutkan modul berikutnya dan satu keputusan pembuka di ringkasan akhir. Ikuti petunjuk regenerasi viewer di `docs/README.md` untuk halaman baru.

## Serahkan ke implementasi

- Jika cakupan dapat dibangun dan diverifikasi dalam satu sesi, berikan `$implement-from-spec <slug>`.
- Jika cakupan perlu beberapa sesi, usulkan tiket berupa bagian fitur end-to-end yang masing-masing dapat didemokan atau diuji. Untuk setiap tiket, tampilkan nomor, hasil, dan prasyarat yang benar-benar menghalanginya. Tambahkan `## Implementation Tickets` ke spec dengan catatan `Menunggu persetujuan pembagian tiket.` Tunggu pengguna menyetujui atau merevisi pembagian sebelum membuat tiket.
- Setelah pembagian disetujui, buat satu file per tiket di `docs/tickets/<slug>/<NN>-<judul>.md`. Cantumkan judul, tautan ke spec induk, bagian **What to Build**, **Acceptance Criteria** berupa checklist hasil teramati, **Blocked By** dengan tautan ke tiket prasyarat atau keterangan bahwa tiket bisa langsung dimulai, serta `Status: ready-for-agent`. Ganti catatan persetujuan di spec dengan tautan tiket. Di Orca, berikan `$implement-from-spec <slug>` untuk mengoordinasikan tiket. Untuk satu tiket yang siap, berikan `$implement-from-spec <slug> <NN>`. Jika pengguna sekaligus meminta mulai implementasi, lanjutkan dengan skill tersebut.
