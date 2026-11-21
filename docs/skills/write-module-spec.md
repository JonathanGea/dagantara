---
name: write-module-spec
description: Gunakan untuk membuat atau memperbarui spesifikasi modul Markdown Dagantara di docs/ dari kebutuhan, catatan, atau keputusan pengguna. Hasilnya siap dirujuk oleh implement-from-spec.
---

# Menulis Spesifikasi Modul Dagantara

## Tetapkan sumber dan cakupan

1. Baca `AGENTS.md`, `docs/README.md`, dan spesifikasi terkait. Pakai catatan serta keputusan yang dirujuk pengguna. Saat merevisi modul, pertahankan keputusan dan ID yang sudah berlaku, lalu sebutkan konflik jika perubahan yang diminta berbenturan dengan spesifikasi terkait.
2. Simpan satu modul per file `docs/<nama-modul>.md`. Saat menambah modul, tambahkan tautannya ke `docs/README.md`. Gunakan pola file modul yang sudah ada sebagai acuan.

## Periksa dependensi saat penulisan

1. Sebelum memfinalkan spesifikasi, telusuri tiap alur dalam cakupan dari kondisi awal sampai hasil yang dapat diuji. Jika alur memerlukan tenant, akun, role, atau permission, cek siapa yang menyiapkannya pertama kali, dari mana konteks tenant diperoleh, dan siapa yang berwenang pada tiap aksi. Cek juga kepemilikan entitas, aturan penautan, dan cara menyiapkan kondisi awal skenario.
2. Cari jawaban di sumber yang sudah disepakati. Gunakan pola repo untuk keputusan teknis yang bisa didelegasikan. Jika keputusan yang memengaruhi perilaku, hak akses, atau skema masih terbuka dan menghalangi alur, ajukan pertanyaan yang spesifik kepada pengguna saat menulis spesifikasi. Jelaskan dampak pilihan jika ada beberapa opsi yang masuk akal. Lanjutkan bagian yang sudah jelas.
3. Tulis keputusan yang sudah disepakati sebagai requirement. Catat keputusan yang masih terbuka sebagai *implementation dependency*, dengan ID terkait, keputusan yang diperlukan, serta alur dan skenario yang tertahan. Jika pengguna meminta rancangan, tandai usulan yang masih membutuhkan konfirmasi.

## Susun spesifikasi yang bisa diuji

Gunakan urutan bagian dari `docs/README.md`.

- **Module Overview** menjelaskan tujuan dan batas modul.
- **Admin UI Structure** mencatat navigasi dan aksi admin yang sudah diketahui. Cantumkan ID Functional Specification pada setiap aksi yang menjadi requirement.
- **Functional Specification** menjelaskan siapa melakukan apa, kondisi awal, dan hasilnya. Gunakan ID stabil seperti `UM-01`.
- **Business Rules** mencatat batas izin, kepemilikan tenant, validasi, dan invarian yang berlaku. Gunakan ID seperti `UM-BR-01`.
- **Data Model** menjelaskan entitas, kepemilikan, kardinalitas, dan constraint yang sudah disepakati. Gunakan Mermaid jika membantu membaca relasi atau alur.
- **Acceptance Scenarios** memakai format `ID | Requirement | Kondisi | Tindakan | Hasil yang diharapkan`. Gunakan ID skenario seperti `UM-AT-01` dan isi `Requirement` dengan ID Functional Specification yang dibuktikan. Buat kondisi awal yang bisa disiapkan, tindakan yang sesuai dengan cakupan resmi, dan hasil yang dapat diamati. Untuk penolakan, sebutkan data yang harus tetap sama atau tidak boleh diungkap. Untuk pengujian permission, uji aksi yang membutuhkan permission tersebut.

## Periksa sebelum selesai

- Pastikan ID unik dan stabil. Gunakan awalan modul yang konsisten dan belum dipakai saat membuat modul baru.
- Periksa bahwa setiap requirement dapat ditelusuri ke skenario atau keputusan yang masih diperlukan. Laporkan bagian yang siap diimplementasikan dan bagian yang masih tertahan beserta ID-nya.
- Cocokkan aksi, aturan, relasi, dan skenario antarbagiannya. Tulis hasil yang diharapkan agar bisa diverifikasi melalui tes HTTP Bruno atau tes backend yang sesuai.
- Laporkan file yang dibuat atau diubah dan ID yang terdampak. Alur yang dependensinya sudah tuntas bisa dirujuk melalui `$implement-from-spec` dengan file modul dan ID terkait. Nyatakan seluruh modul siap diimplementasikan setelah dependensi penghambat di semua alur terselesaikan.
