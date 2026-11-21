---
name: spring-boot-run
description: Gunakan saat diminta menjalankan atau memeriksa backend Spring Boot Dagantara secara lokal. Bukan untuk deployment atau implementasi fitur.
---

# Menjalankan Backend Dagantara

Pastikan repository dan branch yang diminta, lalu baca `AGENTS.md`, `backend/AGENTS.md`, dan `backend/README.md`. Gunakan `backend/pom.xml` untuk target Java. Tugas menjalankan backend lokal tidak mencakup ID Functional Specification atau Acceptance Scenario modul.

Laporkan hasil startup, PID, URL health, dan keterbatasan verifikasi. Hindari menyalin rahasia dari log.

## Orca Chat

Jalankan backend di foreground dalam tab terminal **Dagantara Backend** pada worktree chat. Gunakan `$orca-cli` untuk memilih worktree dan mengelola terminal. Di Windows, gunakan `$env:ORCA_CLI_COMMAND` jika tersedia, atau `orca`.

Dari root worktree, cek port tujuan dan cari judul persis `Dagantara Backend` di `result.terminals` dari `orca terminal list --worktree active --json`. Jika backend dari worktree ini sudah sehat, laporkan PID dan tabnya jika tersedia. Jika proses lain memakai port, laporkan konflik dan biarkan proses tersebut berjalan.

Pakai ulang handle tab yang ditemukan jika tab punya `tabId`, terhubung, dan bisa menerima input (`connected` dan `writable`). Baca tampilan terkininya dengan `orca terminal read --terminal '<handle>' --screen --json`. Kirim perintah hanya jika prompt shell yang sesuai sudah siap dan draf perintah kosong. Jika tab sibuk atau tujuan ambigu, laporkan keadaannya. Gunakan `orca terminal switch --terminal '<handle>'` bila pengguna meminta langsung pindah ke tab yang dipakai ulang.

Buat tab baru hanya bila belum ada tab **Dagantara Backend** di worktree itu. Tambahkan `--focus` bila pengguna meminta langsung pindah ke tab.

```powershell
orca terminal create --worktree active --title 'Dagantara Backend' --shell powershell.exe --json
```

Untuk tab baru, gunakan `result.terminal.handle` setelah pembuatan mengonfirmasi `result.terminal.surface` bernilai `visible`. Jika tab terlihat tidak tersedia, laporkan kendalanya sebelum menjalankan backend.

Kirim perintah ke handle tab yang dipilih. Gunakan path absolut dari worktree agar perintah tetap benar jika direktori kerja tab berubah.

```powershell
$backendRunScript = (Resolve-Path '.agents/skills/spring-boot-run/scripts/run-backend.ps1').Path.Replace("'", "''")
orca terminal send --terminal '<handle>' --text "& '$backendRunScript' -Foreground" --enter --json
```

Di POSIX, buat tab baru tanpa `--shell` bila perlu, lalu kirim `cd '<path absolut ke backend>' && ./mvnw spring-boot:run`.

Gunakan `orca terminal read` pada handle tab untuk memantau build dan startup. Setelah Java mulai, cek `http://localhost:8080/actuator/health` dari sesi chat dan pastikan PID listener berasal dari worktree backend yang diminta. Tinggalkan proses yang sehat di tab. Pengguna bisa menghentikannya dengan `Ctrl+C`.

Gunakan `-Port <nomor>` di Windows hanya bila pengguna meminta port lain, lalu cek health pada port tersebut.

## Windows PowerShell di luar Orca

Dari root repository, jalankan `& .\.agents\skills\spring-boot-run\scripts\run-backend.ps1` untuk mode background. Skrip memvalidasi JDK dan port, membangun JAR dengan Maven Wrapper, lalu menunggu health `UP`. Gunakan `-Foreground` jika pengguna ingin log langsung di terminal. Cek health dari shell lain dalam mode foreground. Untuk kegagalan di mode background, baca `backend/target/run-backend.*.log`.

## Shell POSIX

Gunakan `./mvnw spring-boot:run` dari folder `backend`, biarkan proses tetap hidup, lalu verifikasi `http://localhost:8080/actuator/health`. Periksa port sebelum memulai agar tidak membuka instance kedua.
