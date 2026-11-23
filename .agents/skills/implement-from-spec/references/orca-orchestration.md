# Implementasi tiket dengan Orca

Gunakan skill `orchestration` dan `orca-cli`. Muat panduan dari CLI Orca yang aktif, termasuk referensi coordinator loop untuk dependensi dan low-level topology untuk menautkan Task ke terminal. Pada contoh ini, `orca` berarti executable Orca yang dipilih sesuai panduan tersebut. Ikat operasi lokal dengan `--environment local`. Kenali konteks Orca dari sesi, misalnya keberadaan `ORCA_AGENT_SESSION_ID`, dan pastikan checkout koordinator terdaftar dengan `orca worktree current --environment local --json` sebelum membuat Run atau worker.

## Lanjutkan dari kondisi yang ditemukan

1. Baca spec, daftar tiket tertaut, dan prasyaratnya. Bila pembagian masih menunggu persetujuan, gunakan alur `write-module-spec` untuk menuntaskannya. Persetujuan yang sudah diberikan berlaku untuk pembagian tersebut. Jika pengguna sekaligus meminta implementasi dengan Orca, teruskan ke peluncuran setelah tiket tersimpan. Bila spec siap dikerjakan langsung, gunakan alur satu spec.
2. Inventarisasi branch dan child worktree yang ada, perubahan tracked dan untracked, status agen, serta commit tiket. Cocokkan semua itu dengan spec untuk menentukan pekerjaan yang sudah terintegrasi, sedang berjalan, atau siap dimulai. Pakai branch fitur yang sesuai sebagai branch integrasi jika sudah ada. Pastikan spec, tiket, dan commit prasyarat tersedia pada ref yang akan dipakai worker. Bukti kesiapan adalah hasil tiket yang tersedia di basis kerja. Branch fitur bisa memuat hasil tersebut sebelum PR ke `dev` dibuat.
3. Lanjutkan Run yang sudah terkait dengan spec atau buat satu dengan `orca orchestration run-create --environment local --objective <spec>`. Daftarkan satu Task per tiket dalam urutan dependensi dengan `orca orchestration task-create --environment local --run <run-id> --spec-file <brief> --deps <array-json-id-task>`. Gunakan dependensi di tiket dan pakai kembali Run, Task, serta dispatch yang cocok. Untuk tiket yang sudah terintegrasi sebelum Run dimulai, catat Task sebagai `completed` beserta commit setelah memverifikasi hasilnya di branch integrasi.
4. Susun brief yang bisa dipakai worker di sesi baru. Sertakan path spec dan tiket, kriteria penerimaan, branch dan child yang ditugaskan, commit prasyarat, perubahan yang diserahkan, cakupan file, dan bukti yang harus dikembalikan. Arahkan worker ke `implement-from-spec` untuk cakupan dan ke routing `AGENTS.md` serta `backend/AGENTS.md` bila relevan. Untuk HTTP yang terdampak, rujuk `bruno-api-testing`. Cantumkan di brief bahwa worker mengedit, menguji, dan membuat commit di child yang ditugaskan, sementara chat induk mengoordinasikan dan mengintegrasikan hasil. Tulis pembagian peran ini di brief agar tersedia juga bagi child dengan versi skill lama.

## Pilih model dan effort worker

Koordinator memilih model dan effort Codex CLI per tiket. Gunakan `gpt-6.2-sol` sebagai default.

- `gpt-6-luna` dengan `high` boleh dipilih untuk tiket kecil, terlokalisasi, dan mudah diverifikasi, seperti memperbarui dokumentasi atau menambah satu skenario Bruno mengikuti contoh yang sudah ada.
- `gpt-6.2-sol` dengan `medium` untuk tiket rutin dengan kontrak jelas, seperti perubahan DTO atau validasi yang sudah dispesifikasikan.
- `gpt-6.2-sol` dengan `high` jika tiket mengubah autentikasi, otorisasi, isolasi tenant, migration, transaksi atomik, atau komponen bersama yang berdampak lintas modul. Contohnya provisioning tenant dan delegasi izin operator platform.

Terapkan pilihan model atau effort yang diberikan pengguna. Jika koordinator memilih Luna dan model itu tidak tersedia, gunakan model default dengan effort yang sesuai cakupan. Saat melanjutkan worker aktif yang sudah memegang tiket, gunakan konfigurasi worker tersebut.

## Luncurkan Codex CLI dan kirim tiket

1. Ambil Task yang siap dengan `orca orchestration task-list --environment local --run <run-id> --ready --brief`. Cocokkan commit prasyarat dengan basis setiap worktree. Luncurkan tiket independen yang siap dalam kelompok yang sama sesuai kapasitas Orca, mengikuti referensi parallel start. Contohnya, jika 02 dan 03 masing-masing hanya bergantung pada 01, mulai keduanya setelah hasil 01 tersedia. Tetapkan pemilik file bersama seperti migration dan konfigurasi security sebelum peluncuran. Gunakan kontrak yang sudah disepakati untuk pekerjaan independen, dan jadwalkan perubahan pada file bersama melalui pemiliknya.
2. Gunakan `--surface terminal` untuk membuka CLI secara eksplisit. `worker-start` mengikuti pengaturan surface Orca, sehingga peluncuran terminal dan pengiriman Task dilakukan bertahap. Buat child dan agen dalam satu perintah, dengan branch integrasi sebagai basis. Isi `<model>` dan `<effort>` dengan pilihan untuk tiket itu.

   ```text
   orca worktree create --environment local --parent-worktree <worktree-koordinator> --name <slug-NN> --base-branch <ref-integrasi> --agent codex --model <model> --effort <effort> --surface terminal --json
   ```

   Bila child untuk tiket sudah ada, pakai ulang branch dan perubahan lokalnya. Periksa basis, status Git, serta kepemilikan pekerjaan sebelum menyerahkan tiket. Lanjutkan worker aktif yang sudah memegang Task. Jika agen induk atau agen lain sebelumnya mengedit melalui shell, pastikan pengerjaannya sudah berhenti atau diserahterimakan sebelum membuka worker baru. Setelah worktree siap, pakai terminal Codex yang sesuai atau luncurkan Codex di child itu.

   ```text
   orca agent launch --environment local --worktree <worktree-tiket> --agent codex --model <model> --effort <effort> --surface terminal --json
   ```

3. Dari receipt peluncuran, pastikan yang terbuka adalah terminal agen Codex dan ambil handle-nya. Tunggu TUI siap dengan `orca terminal wait --environment local --terminal <handle> --for tui-idle --timeout-ms 60000 --json` bila diperlukan. Serahkan brief melalui Task Orca, sehingga agen menerima identitas Task dan Dispatch di prompt.

   ```text
   orca orchestration worker-start --environment local --run <run-id> --task <task-id> --terminal <handle> --json
   ```

   Laporkan nomor tiket, child, branch, model, effort, dan status ketika receipt mengonfirmasi worker menerima Task. Worktree, terminal shell, atau keberhasilan membuka proses saja belum membuktikan Task terkirim. Jika launch atau dispatch gagal, ikuti tindakan pemulihan pada receipt sesuai panduan Orca. Gunakan kembali identitas yang tepat dan hindari membuat worker ganda.

## Integrasikan hasil

- Worker mengimplementasikan dan memverifikasi satu tiket di branch child, membuat commit, lalu melapor lewat mekanisme `worker_done`. Brief menegaskan bahwa koordinator menangani integrasi dan PR. Jalankan Bruno paralel hanya bila backend dan database uji terisolasi. Saat fasilitas uji dipakai bersama, jadwalkan verifikasi bergantian pada worktree yang benar.
- Saat Orca mengirim hasil, periksa commit dan bukti tes, lalu integrasikan perubahan yang sudah terverifikasi. Lepaskan tiket turunan setelah commit prasyarat benar-benar tersedia pada basisnya. Setelah mengirim pekerjaan, ikuti mekanisme wake-up Orca untuk melanjutkan koordinasi.
- Setelah semua tiket selesai, verifikasi hasil gabungan, rangkum tiap tiket beserta commit dan hasil tes, lalu ikuti bagian PR ke `dev` dari skill ini dengan branch integrasi.
