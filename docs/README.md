# Dagantara

> **AI-Ready Development Specification** · Multi-tenant e-commerce

Dagantara adalah platform e-commerce multi-tenant. Setiap tenant beroperasi dalam ruang data miliknya sendiri.

## Dokumentasi Modul

- [User Management](user-management.md)
- [Product Management](product-management.md)

## Aturan Membaca untuk AI Developer

1. **Source of truth:** file Markdown modul adalah sumber spesifikasi. `README.md` adalah indeks dan pedoman; `index.html` hanya viewer.
2. **Keputusan final:** implementasikan hanya aturan, fitur, dan relasi yang sudah disepakati di modul. Jangan menjadikan contoh, struktur referensi, atau dugaan sebagai requirement.
3. **Batas navigasi:** entri pada Admin UI Structure menjelaskan lokasi UI. Nama submenu saja tidak otomatis menetapkan fitur, CRUD, API, atau halaman turunannya.
4. **Keputusan yang dibutuhkan:** jika sebuah spesifikasi belum menentukan hal esensial, laporkan dependensinya dan jangan menentukan sendiri. Kerjakan bagian lain yang tidak bergantung pada keputusan tersebut.
5. **Pengerjaan terarah:** implementasikan satu cakupan kecil dengan mengacu pada ID Functional Specification. Jangan menambah fitur di luar cakupan tugas.
6. **Verifikasi:** cocokkan hasil dengan Business Rules, Data Model, dan Acceptance Scenarios yang relevan. Jangan menyimpulkan fitur sudah terimplementasi hanya karena dokumentasinya tersedia.
7. **Konsistensi:** pertahankan aturan tenant isolation, role-based permissions, serta integritas relasi yang telah disepakati.
8. **Penulisan dokumen:** simpan satu modul per file Markdown. Tulis keputusan yang berlaku, bukan alternatif atau fitur yang tidak dipilih; jangan mengulang informasi tanpa kebutuhan.

## Struktur Spesifikasi Modul

| Bagian | Tujuan |
| --- | --- |
| **Module Overview** | Tujuan dan tanggung jawab modul. |
| **Admin UI Structure** | Menu, submenu, halaman, fitur, dan aksi yang diketahui. |
| **Functional Specification** | Perilaku sistem yang diberi ID agar mudah dijadikan tugas AI. |
| **Business Rules** | Invarian, validasi, dan pembatasan yang wajib dipatuhi. |
| **Data Model** | ERD, fungsi tabel, relasi, dan constraint yang telah ditentukan. |
| **Acceptance Scenarios** | Situasi dan hasil terukur untuk menguji implementasi. |

Gunakan diagram Mermaid apabila membantu menjelaskan data atau workflow. Tambahkan detail permission, lifecycle, atau hubungan antarmodul hanya bila sudah ada keputusan yang relevan.

## Penggunaan dalam Pengembangan

Saat meminta AI mengimplementasikan fitur, sebutkan file modul, ID spesifikasi, dan batas pekerjaan. AI harus membaca seluruh aturan terkait sebelum mengubah kode, lalu memverifikasi acceptance scenario yang sesuai.
