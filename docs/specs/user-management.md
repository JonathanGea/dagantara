# User Management — Akses Platform dan Tenant

## Problem Statement

Calon pemilik toko perlu mengajukan tenant, menyepakati alamat tokonya dengan Dagantara, dan menerima akses untuk mulai bekerja. Setelah toko berjalan, pemilik perlu membagi tugas ke staf dan menjaga kendali atas akun adminnya. Tim Dagantara juga perlu memisahkan kewenangan antaroperator, termasuk penyiapan tenant dan bantuan pemulihan password.

## Solution

Dagantara menyediakan pengajuan tenant yang diselesaikan oleh operator berizin. Pemilik pertama menerima URL login toko, username, dan password sementara. Dari URL tersebut, ia bisa masuk, mengganti password, lalu menyiapkan akun dan role untuk timnya.

Akun internal platform dan tenant menggunakan satu role aktif. Backend memeriksa permission untuk setiap tindakan dan membatasi data berdasarkan keanggotaan tenant akun yang terautentikasi. Admin tenant yang kehilangan akses dapat menghubungi Help Center.

## User Stories

1. **UM-01** — Sebagai pengelola akun yang berizin, saya bisa membuat akun staf atau operator dan menetapkan role agar anggota tim bisa mulai bekerja.
2. **UM-02** — Sebagai pengguna baru yang akunnya dibuat pengelola, saya bisa login dengan username dan password sementara lalu memilih password sendiri.
3. **UM-03** — Sebagai pengelola, saya bisa melihat dan mengganti satu role aktif pengguna sehingga aksesnya sesuai dengan tugas saat ini.
4. **UM-04** — Sebagai pengelola role yang berizin, saya bisa membuat role, memilih permission, dan mendelegasikan akses dalam batas kewenangan saya.
5. **UM-05** — Sebagai Admin Platform, saya bisa membedakan operator provisioning, operator Help Center, dan pengelola akun lewat permission role.
6. **UM-06** — Sebagai admin atau staf tenant, saya bisa login di URL toko dan mengakses data tenant saya sesuai izin.
7. **UM-07** — Sebagai tim Dagantara, saya bisa menyiapkan Admin Platform pertama dengan aman agar pengelolaan operator punya titik awal yang terlindungi.
8. **UM-08** — Sebagai calon admin, saya bisa mengajukan toko dan menyepakati URL dengan operator. Sebagai operator berizin, saya juga bisa memulai onboarding langsung dan memberikan akses kepada pemilik pertama setelah URL siap.
9. **UM-09** — Sebagai admin tenant yang lupa password, saya bisa menghubungi Help Center agar operator berizin memulihkan akses setelah memeriksa identitas saya.
10. **UM-10** — Sebagai pengelola akun yang berizin, saya bisa melihat daftar dan detail, mereset, menonaktifkan, mengaktifkan kembali, atau mengarsipkan akun dalam cakupan saya.
11. **UM-11** — Sebagai [Pemilik Tenant](../../GLOSSARY.md#pemilik-tenant), saya bisa memberi dan mencabut role Admin Tenant pada akun lain. Sebagai Admin Platform pertama, saya memiliki kewenangan setara untuk role Admin Platform.

## Implementation Decisions

### Pengajuan dan provisioning tenant

- Pengajuan publik dimulai dari domain Dagantara. Calon admin mengisi data toko, nomor HP, dan usulan domain. Pengajuan bisa diajukan sebelum ada akun tenant.
- Operator yang memiliki izin provisioning meninjau pengajuan dan menghubungi calon admin. Operator bisa memperbarui usulan selama pengajuan berjalan atau menutup pengajuan jika proses batal. Pengajuan yang masih ditinjau atau sudah ditutup belum menghasilkan tenant dan akun yang bisa login.
- Operator juga bisa memulai onboarding langsung. Kedua jalur menghasilkan tenant melalui proses provisioning yang sama setelah operator dan calon pemilik menyepakati URL final dan URL siap digunakan.
- Pilihan utama adalah domain toko. Alamat Dagantara dengan jalur khusus, misalnya `toko.dagantara/nama-toko`, bisa digunakan berdasarkan kesepakatan. Satu alamat login yang aktif harus memetakan secara unik ke satu tenant. Pada alamat Dagantara, hostname dan jalur toko bersama-sama mengidentifikasi tenant.
- Penyelesaian provisioning menyimpan tenant, pemetaan URL login, role bawaan `TENANT_ADMIN`, akun pemilik dengan nomor HP, serta hubungan pemilik ke tenant secara atomik. Untuk jalur pengajuan, penyelesaian juga menandai pengajuan sebagai selesai. Persetujuan ulang atau kegagalan di tengah proses tidak menghasilkan tenant atau akun ganda.
- Tenant yang berhasil diprovisioning mulai berstatus `ACTIVE`. Alamat yang sudah dipetakan tetap dicadangkan selama tenant nonaktif. [Tenant Management](tenant-management.md#status-tenant-dan-pemetaan-url)
- Sistem menghasilkan password sementara. Operator memberikan URL final, username, dan password sementara kepada pemilik. Detail pembuatan domain dan penyiapan frontend merupakan tanggung jawab alur terpisah.

### Login dan konteks tenant

- Pada URL toko, form login menerima username dan password. Backend menentukan tenant kandidat dari pemetaan URL yang sudah disiapkan, lalu mencocokkan username dan password dengan akun yang terdaftar di tenant tersebut. Username unik per tenant. Akun platform memakai username yang unik di lingkup platform dan masuk melalui akses platform.
- Tenant nonaktif menolak login internal dan mencabut sesi tenant yang sudah berjalan. Aktivasi tenant kembali mengizinkan akun yang memenuhi syarat untuk login ulang. [Tenant Management](tenant-management.md#akses-internal-tenant-nonaktif)
- Setelah login, konteks akses berasal dari akun terautentikasi dan `users.tenant_id`. Jika request memakai URL yang memetakan tenant, pemetaan itu juga harus cocok dengan tenant akun. Nilai tenant dari body, query, atau header yang dikirim klien tidak memberi hak akses atas tenant tersebut.
- Untuk operasi tenant, backend memeriksa permission dan membatasi pencarian, perubahan, penghapusan, relasi, dan daftar berdasarkan tenant akun. Filter tenant berlaku sebelum pagination dan penghitungan total. Untuk produk, keputusan [PM-01](product-management.md#functional-specification) menetapkan `products.tenant_id` sebagai pemilik. Akses sumber daya memakai ID dan tenant secara bersamaan.
- Pada operasi yang dilindungi, respons `401` berlaku saat autentikasi tidak valid dan `403` saat akun terautentikasi kurang izin. Pencarian objek di luar cakupan tenant memberikan `404` seperti objek yang tidak ditemukan di tenant sendiri.

### Role, permission, dan akun yang dilindungi

- `users.role_id` menunjuk satu role aktif. Permission efektif berasal dari `roles → role_permissions → permissions` dengan pasangan role dan permission unik. Akun serta role platform berada di cakupan platform. Akun tenant menggunakan role yang dimiliki tenant yang sama.
- Pada kedua cakupan, pengelola memilih permission dari katalog yang disediakan Dagantara. Pembuatan role, perubahan permission, dan penetapan role diperiksa secara terpisah. Penggantian permission atau role menggunakan kewenangan terhadap izin yang sedang dikelola dan izin baru yang diminta. Operator platform yang mempunyai `tenant_id = NULL` memperoleh akses melalui permission eksplisit.
- Permission pengelolaan akun diberikan per tindakan. `users.create` mengizinkan pembuatan akun, `users.read` mengizinkan daftar dan detail, `users.role.assign` mengizinkan penetapan role, `roles.create` mengizinkan pembuatan role, dan `roles.permissions.update` mengizinkan penggantian permission role. Aktivasi kembali, penonaktifan, reset password, dan pengarsipan juga memiliki izin tersendiri. Admin bawaan memperoleh permission pengelolaan di cakupannya melalui role.
- Untuk tenant, akun yang dibuat saat provisioning menjadi satu-satunya Pemilik Tenant dan memegang role `TENANT_ADMIN`. Dagantara menyimpan status pemilik secara eksplisit. Hanya pemilik yang dapat memberi atau mencabut role bawaan tersebut pada akun lain, termasuk bila role itu ditetapkan pada saat pembuatan akun. Saat menunjuk admin tambahan, pemilik mencatat nomor HP yang akan dipakai untuk pemulihan.
- Pengelolaan staf biasa tidak boleh mengubah, mereset, menonaktifkan, atau mengarsipkan akun Admin Tenant. Pemilik berwenang mengganti role admin tambahan melalui aturan penetapan role admin. Akun pemilik sendiri dilindungi dari penggantian role, penonaktifan, dan pengarsipan. Ganti password sendiri dan reset melalui Help Center mengikuti alur kredensial.
- Admin Platform pertama dibuat saat setup Dagantara. Penyiapan ulang mempertahankan satu akun awal. Akun ini satu-satunya yang dapat memberi atau mencabut role Admin Platform pada akun lain dan dilindungi dari perubahan role, penonaktifan, pengarsipan, serta reset melalui pengelolaan operator biasa. Role sistem bawaan tetap tersedia sebagai dasar pemeriksaan izin.
- Admin Platform bisa membuat akun operator, membuat role, memilih permission, dan mendelegasikan pengelolaan di platform. Role Admin Platform memiliki izin provisioning dan izin reset admin tenant, yang dapat diberikan secara terpisah kepada role operator. Reset Help Center memberi akses terbatas pada pemulihan akun target, termasuk informasi yang diperlukan untuk memeriksa permintaan.

### Siklus akun dan pemulihan password

- Akun staf dan operator dikelola di cakupan masing-masing. Pengelola yang memiliki izin bisa melihat daftar dan detail, membuat akun, menetapkan role, menonaktifkan, mengaktifkan kembali, mereset password, atau mengarsipkan akun sesuai perlindungan dan batas delegasi.
- Penonaktifan mencabut akses dan bisa dibalik dengan izin aktivasi kembali. Aksi hapus mengarsipkan akun, mencabut akses, dan menyimpan identitas serta riwayatnya. Akun arsip tidak muncul pada daftar aktif dan tidak bisa diaktifkan kembali melalui aksi biasa.
- Akun yang dibuat oleh pengelola dan akun yang direset menerima password sementara buatan sistem. Login dengan password sementara hanya memberikan akses untuk mengganti password. Setelah perubahan berhasil, password sementara ditolak. Backend menyimpan hash password dan menjaga kredensial di luar log serta audit.
- Admin tenant yang lupa password menghubungi Help Center. Operator memeriksa pemohon dengan mencocokkan nomor masuk. Jika berbeda, operator dapat menelepon balik nomor yang tercatat atau mengonfirmasi langsung bila mengenal admin. Reset membutuhkan verifikasi operator dan permission reset admin tenant.
- Operator memicu reset dan memberikan password sementara kepada admin yang telah diperiksa. Catatan audit memuat operator, akun target, dan waktu tindakan. Nomor HP dicatat saat onboarding atau penunjukan admin tambahan. Pada versi ini, pemeriksaan identitas berlangsung saat permintaan reset. Informasi proses pemulihan ditempatkan di alur Help Center, di luar halaman login.
- Reset, penonaktifan, dan pengarsipan mencabut sesi yang sebelumnya memberi akses operasional. Setelah perubahan role atau permission, operasi berikutnya mengikuti kewenangan efektif terbaru.

## Testing Decisions

Gunakan API HTTP melalui Bruno untuk menguji alur pemohon, operator platform, dan pengguna tenant dengan fixture terisolasi. Gunakan jalur login yang diselesaikan melalui pemetaan URL per tenant. Gunakan kontrak Product Management untuk menguji kepemilikan produk ketika API produknya tersedia. Periksa hasil penolakan dan keadaan data setelahnya. Untuk halaman login yang memakai kontrak ini, periksa bahwa form berisi username dan password dan informasi proses reset berada di Help Center.

| ID | User Stories | Perilaku yang harus teramati |
| --- | --- | --- |
| UM-AT-01 | UM-01, UM-02, UM-03 | Pengelola berizin membuat akun dengan role yang sah di cakupannya. Login pertama hanya mengizinkan ganti password. Password baru bisa dipakai untuk login biasa. |
| UM-AT-02 | UM-01, UM-05 | Akun tanpa `users.create` ditolak dan calon akun tidak tersimpan. Request tanpa autentikasi juga ditolak. |
| UM-AT-03 | UM-03, UM-04, UM-05 | Pengelola membuat role, menetapkan permission, lalu menempatkan akun di role itu. Aksi berikutnya mengikuti permission efektif yang baru. |
| UM-AT-04 | UM-06 | Akun A memenuhi izin dan berhasil membaca atau mengubah data A. Akses objek B melalui ID, penulisan, dan daftar terisolasi. Lookup objek B memberikan `404` tanpa mengungkap data B. |
| UM-AT-05 | UM-02, UM-04, UM-06 | Pembuatan akun dan penggantian role menolak role dari scope atau tenant lain. Akun dan assignment sebelumnya tidak berubah. |
| UM-AT-06 | UM-07, UM-11 | Setup ulang tidak menggandakan akun Admin Platform pertama. Akun tersebut bisa login, dapat menunjuk admin lain, dan terlindungi dari pengelolaan biasa. |
| UM-AT-07 | UM-08, UM-11 | Pengajuan yang sedang ditinjau belum menghasilkan akses. Setelah URL siap, operator berizin menyelesaikan provisioning. Uji juga jalur yang bermula dari operator, konflik URL, dan kegagalan yang membatalkan seluruh operasi. |
| UM-AT-08 | UM-03, UM-04, UM-05 | Pengelola tidak dapat memberi atau mengganti izin di luar kewenangannya. Operator dengan izin provisioning saja ditolak saat mencoba reset admin. |
| UM-AT-09 | UM-02, UM-06, UM-08 | Uji URL domain toko dan alamat Dagantara dengan jalur toko. Username yang sama dapat dimiliki dua tenant. Akun hanya berhasil masuk melalui URL tenant tempat akun terdaftar dan sesinya ditolak di URL tenant lain. |
| UM-AT-10 | UM-09 | Operator berizin menyelesaikan reset setelah memeriksa pemohon. Password serta sesi lama kehilangan akses, password sementara hanya membuka alur ganti password, dan audit tercatat. Permintaan tanpa verifikasi atau izin ditolak. |
| UM-AT-11 | UM-10 | Pengelola dengan izin yang tepat dapat menonaktifkan dan mengaktifkan kembali akun. Akun arsip kehilangan akses, keluar dari daftar aktif, tetap memiliki riwayat, dan ditolak saat dicoba diaktifkan kembali. |
| UM-AT-12 | UM-04, UM-11 | Pemilik dapat menunjuk admin tambahan dengan nomor HP dan mencabut role Admin Tenant. Pengelola biasa ditolak saat mencoba tindakan itu atau mengelola akun admin. Uji perlindungan pemilik dan kewenangan khusus Admin Platform pertama. |
| UM-AT-13 | UM-08 | Operator bisa menutup pengajuan yang batal. Pengajuan tertutup tidak dapat diprovisioning dan tidak memberi akses tenant. |

## Out of Scope

- Identitas pembeli storefront dan akun pelanggan dibahas di Customer Management. Akun pembeli disimpan terpisah dari akun internal.
- Pembuatan domain, konfigurasi DNS dan HTTPS, serta deployment frontend sesuai tema toko dibahas terpisah. User Management menggunakan URL login yang telah disiapkan.
- Transfer kepemilikan, penggunaan email, dan verifikasi nomor HP dengan kode sekali pakai merupakan enhancement.

## Further Notes

- `POST /api/v1/auth/login` menerima username dan password. Backend menentukan tenant dari hostname dan prefiks toko yang tersimpan sebagai pasangan unik. Detail alur tersedia di [tiket onboarding](../tickets/user-management/02-onboarding-tenant.md) dan [panduan backend](../../backend/README.md#pengajuan-dan-onboarding-tenant-um-02-um-06-um-08-um-11).
- Saat memigrasikan tenant lama, siapkan pemetaan URL sebelum mengalihkan login mereka. Kode role sistem `SUPERADMIN` merepresentasikan Admin Platform.

## Implementation Tickets

| No. | Hasil yang bisa didemokan | Prasyarat |
| --- | --- | --- |
| [01](../tickets/user-management/01-bootstrap-platform-admin.md) | Admin Platform pertama bisa disiapkan ulang tanpa duplikasi dan login dengan username serta password. | Bisa langsung dimulai. |
| [02](../tickets/user-management/02-onboarding-tenant.md) | Operator menyelesaikan pengajuan atau onboarding langsung. Pemilik masuk dari URL toko dan mengganti password sementara. | 01 untuk autentikasi operator provisioning. |
| [03](../tickets/user-management/03-operator-platform.md) | Admin Platform membuat operator dan role, mendelegasikan permission, dan mengelola siklus akun operator. | 01 untuk akun Admin Platform pertama. |
| [04](../tickets/user-management/04-akun-dan-role-tenant.md) | Pemilik dan staf mengelola akun serta role tenant sesuai izin, dengan perlindungan akun pemilik dan admin. | 02 untuk identitas tenant, akun pemilik, dan konteks login. |
| [05](../tickets/user-management/05-pemulihan-help-center.md) | Operator Help Center yang berizin memulihkan akun admin awal atau tambahan dengan verifikasi, password sementara, dan audit. | 02 untuk akun pemilik, 03 untuk role operator Help Center, dan 04 untuk admin tambahan. |
