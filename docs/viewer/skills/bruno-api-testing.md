---
name: bruno-api-testing
description: Buat, tinjau, atau jalankan smoke test dan skenario penerimaan API HTTP Dagantara dengan Bruno dan koleksi bruno/.
---

# Pengujian API HTTP dengan Bruno

Baca spec atau tiket, kontrak HTTP, dan koleksi `bruno/` yang terkait. Untuk setiap perilaku API yang baru atau berubah dalam cakupan, cantumkan rujukan ke kriteria penerimaan atau ID skenario, seperti `UM-AT-01`. Tentukan hasil yang diharapkan dari spec, kontrak, dan fixture yang diketahui sebelum melihat respons aktual. Jika ada keputusan domain yang masih terbuka, kerjakan skenario yang sudah jelas dan laporkan kriteria yang tertahan.

## Buktikan perilaku per kasus

| Bukti | Yang diperiksa |
| --- | --- |
| Kondisi awal | Pastikan login, permission, tenant, dan data awal sesuai skenario. Untuk menguji penolakan izin, gunakan identitas yang berhasil login dan payload serta target yang valid. Saat menguji batas tenant, pastikan target memang ada melalui pelaku yang berwenang. |
| Status HTTP | Assert status yang ditetapkan kontrak untuk aksi itu, termasuk pada kasus penolakan. |
| Kontrak respons | Assert field atau kode error yang menunjukkan hasil. Bandingkan nilai bisnis dengan fixture. Untuk ID atau waktu yang dibuat server, cek bentuk atau relasi yang dijanjikan. Periksa header saat menjadi bagian kontrak. |
| Keadaan akhir | Untuk aksi tulis, baca kembali lewat API yang tersedia. Untuk penolakan, buktikan data tetap sama, entitas baru tidak tersimpan, atau data yang dilindungi tidak muncul, sesuai kriteria. Pakai integration test melalui `spring-boot-testing` untuk efek yang tidak bisa diamati melalui API yang ada. |

Contoh `UM-AT-01` memerlukan bukti bahwa akun tersimpan di tenant pelaku dan role aktifnya sama dengan pilihan saat setup. Tes harus gagal apabila akun tersimpan di tenant yang salah meskipun status HTTP sukses. `UM-AT-02` memerlukan bukti bahwa pelaku yang terautentikasi tanpa `users.create` ditolak dan akun tidak tersimpan.

## Susun skenario yang bisa diulang

- Simpan smoke test di `bruno/smoke/`. Gunakan satu folder `bruno/<modul-atau-slug>/<ID-atau-nomor>-<nama-skenario>/` per kasus. Cantumkan rujukan spec dan kriteria di nama atau deskripsi request aksi. Setiap folder harus bisa dijalankan sendiri.
- Untuk beberapa request, susun setup, aksi, verifikasi, dan cleanup sesuai kebutuhan. Gunakan awalan angka pada nama file `.bru` yang sama dengan `seq`. Beri nama assertion sesuai hasil yang dibuktikan. Validasi respons setup sebelum memakai `bru.setVar` untuk meneruskan ID atau token. Jika setup gagal, hentikan request yang bergantung padanya.
- Gunakan `{{baseUrl}}` dari environment dan simpan kredensial uji di secret lokal atau variabel proses. Gunakan fixture unik tiap run, database uji terisolasi untuk perubahan data, dan cara membersihkan fixture yang tetap tersedia jika run terhenti.

## Jalankan dan laporkan

1. Gunakan environment `Local` dan pastikan target adalah backend worktree yang sedang diuji. Gunakan `spring-boot-run` bila perlu menyiapkan atau memeriksa backend lokal. Saat dipanggil dari `implement-from-spec`, lanjutkan verifikasi lokal dalam cakupan sebagai bagian dari tugas tersebut.
2. Dari folder `bruno/`, jalankan `bru run smoke --env Local`. Setelah smoke lulus, jalankan `bru run <folder-skenario> --env Local` untuk setiap skenario HTTP dalam cakupan. Urutkan request yang saling bergantung. Gunakan `--bail` bila pembersihan tetap bisa dijalankan setelah run berhenti.
3. Laporkan rujukan kriteria, perintah aktual, serta hasil `pass`, `fail`, atau `not run`. Bedakan kegagalan assertion fitur dari setup yang gagal sebelum aksi. Jika membuat report, hilangkan header dan payload yang memuat rahasia atau PII.

[Pola assertion Bruno](https://github.com/bruno-collections/bruno-agent-skills/blob/main/bruno-test-writer/references/testing-patterns.md) · [Opsi Bruno CLI](https://docs.usebruno.com/bru-cli/run/options)
