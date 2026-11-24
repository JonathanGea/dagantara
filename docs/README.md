# Dagantara

> **AI-Ready Development Specification** · Multi-tenant e-commerce

Dagantara adalah platform e-commerce multi-tenant. Setiap tenant beroperasi dalam ruang data miliknya sendiri.

## Dokumentasi Modul

- [Module Roadmap & Ownership](module-roadmap.md)
- [Development Dependencies & Parallel Workstreams](development-dependencies.md)

## Spesifikasi Perubahan

Spesifikasi yang disepakati ditautkan di bagian ini. Judul atau nama file tanpa ekstensi dapat dipakai untuk memilih spec.

- [User Management — Akses Platform dan Tenant](user-management.md)
- [Product Management](product-management.md)
- [Inventory Management](inventory-management.md)

## Aturan Membaca untuk AI Developer

1. Ambil cakupan dari spesifikasi perubahan yang disetujui dan aturan domain dari dokumen modul. Gunakan `README.md` sebagai indeks dan `index.html` sebagai viewer.
2. Implementasikan keputusan yang disetujui. Gunakan contoh dan struktur referensi untuk memahami konteksnya.
3. **Batas navigasi:** entri pada Admin UI Structure menjelaskan lokasi UI. Nama submenu saja tidak otomatis menetapkan fitur, CRUD, API, atau halaman turunannya.
4. **Keputusan yang dibutuhkan:** jika sebuah spesifikasi belum menentukan hal esensial, laporkan dependensinya dan jangan menentukan sendiri. Kerjakan bagian lain yang tidak bergantung pada keputusan tersebut.
5. Ambil satu cakupan dari spec, tiket, atau ID Functional Specification yang diminta.
6. Verifikasi perilaku dengan Testing Decisions, Acceptance Criteria, atau Acceptance Scenarios yang relevan.
7. **Konsistensi:** pertahankan aturan tenant isolation, role-based permissions, serta integritas relasi yang telah disepakati.
8. Simpan satu perubahan per spec Markdown. Tautkan dokumen modul untuk aturan yang berlaku lintas perubahan.

## Referensi Format Modul

| Bagian | Tujuan |
| --- | --- |
| **Module Overview** | Tujuan dan tanggung jawab modul. |
| **Admin UI Structure** | Menu, submenu, halaman, fitur, dan aksi yang diketahui. |
| **Functional Specification** | Perilaku sistem yang diberi ID agar mudah dijadikan tugas AI. |
| **Business Rules** | Invarian, validasi, dan pembatasan yang wajib dipatuhi. |
| **Data Model** | ERD, fungsi tabel, relasi, dan constraint yang telah ditentukan. |
| **Acceptance Scenarios** | Kondisi, tindakan, dan hasil terukur yang ditautkan ke ID requirement. |

Gunakan diagram Mermaid apabila membantu menjelaskan data atau workflow. Tambahkan detail permission, lifecycle, atau hubungan antarmodul hanya bila sudah ada keputusan yang relevan.

## Penggunaan dalam Pengembangan

[Panduan Penggunaan Skill](skill-guide.md) memuat cara memilih skill dan contoh prompt siap salin untuk setiap skill di repository.

1. Panggil `$write-module-spec <ide>`. AI menelusuri repo, mengajukan pertanyaan dengan rekomendasi, dan merangkum keputusan untuk kamu konfirmasi sebelum menulis spec di `docs/<slug>.md`.
2. Gunakan `$design-ui` jika perlu meninjau layout atau alur UI. Sertakan spec, layar yang sudah ada, atau brief. Layout dasar bisa dimulai dari brief dan satu contoh tugas pengguna.
3. Gunakan `$implement-from-spec <slug>` untuk spec yang siap dikerjakan. Jika spec menautkan tiket yang sudah disetujui, panggilan ini mengoordinasikan worker di sesi Orca. Gunakan `$implement-from-spec <slug> <NN>` untuk memilih satu tiket yang sudah bisa dimulai.
4. Lanjutkan di sesi yang sama atau buka sesi baru yang punya akses ke spec, tiket, dan perubahan prasyarat. Satu sesi per tiket membantu menjaga fokus pada hasil yang akan diverifikasi.

Gunakan `$implement-from-spec product-management` atau `$implement-from-spec inventory-management` untuk memilih salah satu spec. Tambahkan aksi, ID requirement, atau ID skenario untuk mempersempit cakupan.

### Daftar skill pada viewer

Viewer mengambil skill dari setiap folder `.agents/skills/` yang memiliki `SKILL.md`. Setelah mengubah skill, panduan, indeks ini, atau spec Product dan Inventory Management, jalankan `powershell -ExecutionPolicy Bypass -File .\docs\generate-skills.ps1` dari root repository. Skrip memperbarui manifest, salinan publik skill, dan data cadangan di `index.html` untuk akses saat viewer dibuka langsung.
